package com.atguigu.gmall.index.aspect;

import org.springframework.transaction.TransactionDefinition;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    /**
     * 缓存key的前缀
     * @return
     */
    String prefix() default "";

    /**
     * 缓存的过期时间
     * 单位：分钟
     * @return
     */
    int timeout() default 5;

    /**
     * 为了防止缓存雪崩，可以指定随机值范围
     * 单位：分钟
     * @return
     */
    int random() default 5;

    /**
     * 为了防止缓存穿透，添加分布式锁
     * 通过该属性可以指定分布式锁的名称
     * @return
     */
    String lock() default "lock";
}
