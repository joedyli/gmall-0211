package com.atguigu.gmall.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component
public class CartAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY = "cart:async:exception";

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        log.error("有一个子任务出现了异常。异常信息：{}，异常方法：{}，方法参数：{}", throwable.getMessage(), method, objects);

        String userId = objects[0].toString();
        if (StringUtils.isNotBlank(userId)){
            BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(KEY);
            listOps.leftPush(userId);
        } else {
            throw new RuntimeException("该用户的购物车异步执行失败，并且没有传递用户信息！");
        }
    }
}
