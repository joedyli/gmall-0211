package com.atguigu.gmall.wms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack){
                log.error("消息没有到达交换机");
            }
        });
        this.rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("消息没有到达队列，交换机：{}，路由键：{}，消息内容：{}", exchange, routingKey, new String(message.getBody()));
        });
    }

    @Bean
    public Queue ttlQueue(){
        return QueueBuilder.durable("STOCK-TTL-QUEUE").withArgument("x-message-ttl", 120000)
                .withArgument("x-dead-letter-exchange", "ORDER-EXCHANGE").withArgument("x-dead-letter-routing-key", "stock.unlock").build();
    }

    @Bean
    public Binding ttlBinding(){
        return new Binding("STOCK-TTL-QUEUE", Binding.DestinationType.QUEUE,
                "ORDER-EXCHANGE", "stock.ttl", null);
    }
}
