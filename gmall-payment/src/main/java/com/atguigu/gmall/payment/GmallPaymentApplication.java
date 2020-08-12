package com.atguigu.gmall.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.atguigu.gmall.payment.mapper")
@EnableFeignClients
public class GmallPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallPaymentApplication.class, args);
    }

}
