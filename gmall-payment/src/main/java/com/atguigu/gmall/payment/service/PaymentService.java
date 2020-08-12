package com.atguigu.gmall.payment.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.Interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.mapper.PaymentMapper;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class PaymentService {

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private PaymentMapper paymentMapper;

    public OrderEntity queryOrderByOrderToken(String orderToken) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.queryOrderByToken(orderToken, userInfo.getUserId());
        return orderEntityResponseVo.getData();
    }

    public Long savePayment(OrderEntity orderEntity) {
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setPaymentStatus(0);
        paymentInfoEntity.setCreateTime(new Date());
        paymentInfoEntity.setTotalAmount(orderEntity.getPayAmount());
        paymentInfoEntity.setSubject("谷粒商城支付平台");
        paymentInfoEntity.setPaymentType(1);
        paymentInfoEntity.setOutTradeNo(orderEntity.getOrderSn());
        this.paymentMapper.insert(paymentInfoEntity);
        return paymentInfoEntity.getId();
    }

    public PaymentInfoEntity queryPaymentById(String paymentId) {
        return this.paymentMapper.selectById(paymentId);
    }

    public void updatePayment(PayAsyncVo payAsyncVo) {
        PaymentInfoEntity paymentInfoEntity = this.paymentMapper.selectById(payAsyncVo.getPassback_params());
        paymentInfoEntity.setPaymentStatus(1);
        paymentInfoEntity.setCallbackTime(new Date());
        paymentInfoEntity.setCallbackContent(JSON.toJSONString(payAsyncVo));
        paymentInfoEntity.setTradeNo(payAsyncVo.getTrade_no());
        this.paymentMapper.updateById(paymentInfoEntity);
    }
}
