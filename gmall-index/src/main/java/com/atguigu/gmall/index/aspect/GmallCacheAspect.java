package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取目标对象类：joinPoint.getTarget().getClass()
     * 获取目标方法参数：joinPoint.getArgs()
     * 获取目标方法：(MethodSignature)joinPoint.getSignature().getMethod()
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.atguigu.gmall.index.aspect.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取目标方法
        Method method = signature.getMethod();
        // 获取目标方法上的注解
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        // 获取注解中的前缀
        String prefix = gmallCache.prefix();
        // 获取目标方法的形参
        Object[] args = joinPoint.getArgs();
        // prefix + args 组成缓存key
        String key = prefix + Arrays.asList(args);

        // 获取方法的返回值类型
        Class returnType = signature.getReturnType();

        // 1.查询缓存，缓存中有，直接反序列化并返回
        String json = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)){
            return JSON.parseObject(json, returnType);
        }

        // 2.加分布式锁
        String lock = gmallCache.lock();
        RLock fairLock = this.redissonClient.getFairLock(lock + args);
        fairLock.lock();

        // 3.再查缓存，有直接返回
        String json2 = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json2)){
            fairLock.unlock();
            return JSON.parseObject(json2, returnType);
        }

        // 4.再去执行目标方法
        Object result = joinPoint.proceed(joinPoint.getArgs());

        // 5.把目标方法的返回值放入缓存
        int timeout = gmallCache.timeout();
        int random = gmallCache.random();
        this.redisTemplate.opsForValue().set(key, JSON.toJSONString(result), timeout + new Random().nextInt(random), TimeUnit.MINUTES);

        // 6.解锁
        fairLock.unlock();

        return result;
    }
}
