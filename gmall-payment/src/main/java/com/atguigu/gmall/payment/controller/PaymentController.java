package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.Interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("pay.html")
    public String topay(@RequestParam("orderToken")String orderToken, Model model){

        OrderEntity orderEntity = this.paymentService.queryOrderByOrderToken(orderToken);
        if (orderEntity == null) {
            throw new OrderException("该用户对应的订单不存在！");
        }
        model.addAttribute("orderEntity", orderEntity);
        return "pay";
    }

    @GetMapping("alipay.html")
    @ResponseBody  // 该方法将以其他视图形式返回：json html xml
    public String alipay(@RequestParam("orderToken")String orderToken){
        // 1. 校验订单的真实性
        OrderEntity orderEntity = this.paymentService.queryOrderByOrderToken(orderToken);
        if (orderEntity == null && orderEntity.getStatus() != 0) {
            throw new OrderException("该用户对应的订单不存在！");
        }
        try {
            // 2. 整合参数
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderToken);
            String price = StringUtils.substring(orderEntity.getPayAmount().toString(), 0, StringUtils.lastIndexOf(orderEntity.getPayAmount().toString(), ".") + 3);
            payVo.setTotal_amount(price); // 填一个模拟数据
            //payVo.setTotal_amount("0.01");
            payVo.setSubject("xxxxxxx");

            // 3. 记录对账表
            Long paymentId = this.paymentService.savePayment(orderEntity, price);
            payVo.setPassback_params(paymentId.toString());

            // 4. 调用阿里接口，获取支付表单
            return this.alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        throw new OrderException("支付接口调用异常，请稍后再试！");
    }

    @PostMapping("pay/success")
    public String paySuccess(PayAsyncVo payAsyncVo){
        // 1.验签
        Boolean flag = this.alipayTemplate.verifySignature(payAsyncVo);
        if (!flag){
            return "failure";
        }

        // 2.校验业务参数：app_id、out_trade_no、total_amount
        String out_trade_no = payAsyncVo.getOut_trade_no();
        String app_id = payAsyncVo.getApp_id();
        String total_amount = payAsyncVo.getTotal_amount();
        String paymentId = payAsyncVo.getPassback_params();
        if (StringUtils.isBlank(paymentId)){
            return "failure";
        }
        PaymentInfoEntity paymentInfoEntity = this.paymentService.queryPaymentById(paymentId);
        if (!StringUtils.equals(out_trade_no, paymentInfoEntity.getOutTradeNo())
                || !StringUtils.equals(app_id, this.alipayTemplate.getApp_id())
                || paymentInfoEntity.getTotalAmount().compareTo(new BigDecimal(total_amount)) != 0){
            return "failure";
        }

        // 3.判断交易状态：TRADE_SUCCESS
        if (!StringUtils.equals("TRADE_SUCCESS", payAsyncVo.getTrade_status())){
            return "failure";
        }

        // 4.完成业务处理
        // 4.1. 更新对账记录，已付款
        this.paymentService.updatePayment(payAsyncVo);

        // 4.2. 更新订单状态，待发货（减库存）
        this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "order.success", payAsyncVo.getOut_trade_no());

        // 5.返回success
        return "success";
    }

    @GetMapping("pay/ok")
    public String payOk(){

        return "paysuccess";
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("seckill/{skuId}")
    public ResponseVo<Object> seckill(@PathVariable("skuId")Long skuId){
        UserInfo userInfo = LoginInterceptor.getUserInfo();
//        RSemaphore semaphore = this.redissonClient.getSemaphore("semaphore");
//        semaphore.trySetPermits(100);

        RLock fairLock = this.redissonClient.getFairLock("lock:" + skuId);
        fairLock.lock();

        String stockString = this.redisTemplate.opsForValue().get("seckill:stock:" + skuId);
        if (StringUtils.isBlank(stockString) || new Integer(stockString) <= 0){
            throw new RuntimeException("手慢了，秒杀已结束！");
        }
        // 从redis中减库存
        this.redisTemplate.opsForValue().decrement("seckill:stock:" + skuId);

        // 发送消息给oms创建订单，oms中创建订单成功后减库存
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userInfo.getUserId());
        map.put("skuId", skuId);
        map.put("count", 1);
        this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "sec:kill", map);

        // 防止订单没有创建成功就查询订单
        RCountDownLatch countdown = this.redissonClient.getCountDownLatch("countdown:" + skuId);
        countdown.trySetCount(1);

        fairLock.unlock();

        return ResponseVo.ok();
    }

    @GetMapping("seckill/success/{skuId}")
    public ResponseVo<Object> queryOrder(@PathVariable("skuId")Long skuId) throws InterruptedException {
        RCountDownLatch countdown = this.redissonClient.getCountDownLatch("countdown:" + skuId);
        countdown.await();

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        // 根据用户信息查询当前的秒杀订单 TODO

        return ResponseVo.ok(null);
    }
}
