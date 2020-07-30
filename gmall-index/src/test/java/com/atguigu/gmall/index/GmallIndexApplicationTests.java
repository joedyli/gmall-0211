package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class GmallIndexApplicationTests {

    //@Autowired
    //private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        this.redisTemplate.opsForValue().set("name1", "marong");
        //System.out.println(this.redisTemplate.opsForValue().get("name"));
    }

}
