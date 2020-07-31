package com.atguigu.gmall.index.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DistributeLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Thread thread;

    public Boolean tryLock(String lockName, String uuid, Long expire){
        String script = "if (redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1) " +
                "then redis.call('hincrby', KEYS[1], ARGV[1], 1); redis.call('expire', KEYS[1], ARGV[2]); return 1; " +
                "else return 0; end;";
        if(this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid, expire.toString()) == 0){
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tryLock(lockName, uuid, expire);
        }
        renewTime(lockName, uuid, expire);
        return true;
    }

    public void unLock(String lockName, String uuid){
        String script = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return nil end; " +
                "if (redis.call('hincrby', KEYS[1], ARGV[1], -1) > 0) then return 0 " +
                "else redis.call('del', KEYS[1]) return 1 end;";
        if(this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid) == null){
            throw new IllegalArgumentException("attempt to unlock lock, lockname: " + lockName + ", uuid: " + uuid);
        }
        this.thread.interrupt();
    }

    private void renewTime(String lockName, String uuid, Long expire){
        String script = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 1) " +
                "then return redis.call('expire', KEYS[1], ARGV[2]) end;";
        this.thread = new Thread(() -> {
            while(true){
                try {
                    Thread.sleep(expire *  1000 * 2 / 3);
                    this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid, expire.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        this.thread.start();
    }
}
