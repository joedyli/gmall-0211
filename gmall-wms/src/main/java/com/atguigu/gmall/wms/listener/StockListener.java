package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class StockListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    private static final String KEY_PREFIX = "wms:lock:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER-STOCK-QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.fail", "stock.unlock"}
    ))
    public void unlockStock(String orderToken, Channel channel, Message message) throws IOException {

        String skuLockString = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        // 如果没有缓存的库存信息，则直接返回
        if (StringUtils.isBlank(skuLockString)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return ;
        }

        try {
            // 解锁库存
            List<SkuLockVo> skuLockVos = JSON.parseArray(skuLockString, SkuLockVo.class);
            skuLockVos.forEach(skuLockVo -> {
                this.wareSkuMapper.unlock(skuLockVo.getWareSkuId(), skuLockVo.getCount());
            });

            // 删除锁定库存的缓存（防止重复解锁库存）
            this.redisTemplate.delete(KEY_PREFIX + orderToken);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()){
                log.error("解锁库存的消息消费失败，订单编号：" + orderToken);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}
