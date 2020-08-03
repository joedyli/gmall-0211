package com.atguigu.gmall.item.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(
            @Value("${threadPool.coreSize}")Integer coreSize,
            @Value("${threadPool.maxSize}")Integer maxSize,
            @Value("${threadPool.timeOut}")Integer timeOut,
            @Value("${threadPool.blockingSize}")Integer blockingSize
    ){
        return new ThreadPoolExecutor(coreSize, maxSize, timeOut, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(blockingSize));
    }
}
