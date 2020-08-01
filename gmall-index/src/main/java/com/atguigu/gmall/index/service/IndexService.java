package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributeLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsClient pmsClient;

    private static final String KEY_PREFIX = "index:category:";

    @Autowired
    private DistributeLock distributeLock;

    @Autowired
    private RedissonClient redissonClient;

    public List<CategoryEntity> queryLv1lCategories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0l);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }

    @GmallCache(prefix = KEY_PREFIX, lock = "lock", timeout = 43200, random = 10080)
    public List<CategoryEntity> queryCategoriesWithSubByPid(Long pid) {
        // 再远程查询数据库，并放入缓存
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();

        return categoryEntities;
    }

    public List<CategoryEntity> queryCategoriesWithSubByPid2(Long pid) {
        // 先查询缓存
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseArray(json, CategoryEntity.class);
        }

        // 为了防止缓存击穿，添加分布式锁
        RLock lock = this.redissonClient.getFairLock("lock");
        lock.lock();

        // 再查询缓存，缓存中有，直接返回
        String json2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json2)) {
            lock.unlock();
            return JSON.parseArray(json2, CategoryEntity.class);
        }

        // 再远程查询数据库，并放入缓存
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        // 为了解决缓存穿透，数据即使为null也要缓存；为了防止缓存雪崩，给缓存时间添加随机值
        this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 30 + new Random().nextInt(10), TimeUnit.DAYS);

        // 释放分布式锁
        lock.unlock();

        return categoryEntities;
    }

    public void testLock() {
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock(50, TimeUnit.SECONDS);
        String numString = this.redisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(numString)) {
            return;
        }
        Integer num = Integer.parseInt(numString);
        this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

        testSubLock();

        lock.unlock();
    }

    private void testSubLock() {
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock(50, TimeUnit.SECONDS);
        System.out.println("这是一个子方法，也需要获取锁。。。。。。");
        lock.unlock();
    }

    public void testLock3() {
        String uuid = UUID.randomUUID().toString();
        this.distributeLock.tryLock("lock", uuid, 30l);
        String numString = this.redisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(numString)) {
            return;
        }
        try {
            TimeUnit.SECONDS.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Integer num = Integer.parseInt(numString);
        this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

        this.testSubLock3("lock", uuid);

        this.distributeLock.unLock("lock", uuid);
    }

    private void testSubLock3(String lockName, String uuid) {

        this.distributeLock.tryLock(lockName, uuid, 30l);

        System.out.println("这是一个子方法，也需要获取锁。。。。。。");

        this.distributeLock.unLock(lockName, uuid);
    }

    public void testLock2() {
        // 1.获取锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        // 判断
        if (!lock) {
            try {
                // 3.没有获取到锁，重试
                Thread.sleep(200);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            // 2.获取到锁执行业务逻辑
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                return;
            }
            Integer num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            this.testSubLock2();

            // 释放锁
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then return redis.call('del', KEYS[1]) " +
                    "else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);
//            if (StringUtils.equals(uuid, this.redisTemplate.opsForValue().get("lock"))){
//                this.redisTemplate.delete("lock");
//            }
        }
    }

    private void testSubLock2() {
        // 1.获取锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        // 判断
        if (!lock) {
            try {
                // 3.没有获取到锁，重试
                Thread.sleep(200);
                testSubLock2();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            // 2.获取到锁执行业务逻辑
            System.out.println("这是一个子方法，也需要获取锁。。。。。。");
            // 释放锁
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then return redis.call('del', KEYS[1]) " +
                    "else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);
        }
    }

    public String testWrite() {

        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10, TimeUnit.SECONDS);

        this.redisTemplate.opsForValue().set("msg", UUID.randomUUID().toString());

        //rwLock.writeLock().unlock();
        return "写入成功。。。。。";
    }

    public String testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10, TimeUnit.SECONDS);

        String msg = this.redisTemplate.opsForValue().get("msg");

        //rwLock.readLock().unlock();
        return "读取成功。。。" + msg;
    }

    public String testSemaphore() {
        RSemaphore semaphore = this.redissonClient.getSemaphore("semaphore");
        semaphore.trySetPermits(3);

        try {
            semaphore.acquire(1);

            Thread.sleep(500);
            semaphore.release();

            return "获取资源成功。。。。";
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String testCountDown() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.countDown();

        return "出来了一位同学。。。";
    }

    public String testLatch() throws InterruptedException {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.trySetCount(6);

        latch.await();

        return "班长锁门。。。。。。";
    }
}
