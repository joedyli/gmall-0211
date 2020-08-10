package com.atguigu.gmall.scheduled.jobhandler;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.atguigu.gmall.scheduled.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartJobHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY = "cart:async:exception";
    private static final String KEY_PREFIX = "cart:info:";

    @Autowired
    private CartMapper cartMapper;

    @XxlJob("cartJobHandler")
    public ReturnT<String> executor(String param){

        // 读取失败用户信息
        BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(KEY);
        String userId = listOps.rightPop();
        while (StringUtils.isNotBlank(userId)){

            // 先删除mysql中该用户的购物车
            this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId));

            // 读取redis中该用户的购物车信息
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartJsons = hashOps.values();
            if (CollectionUtils.isEmpty(cartJsons)){
                userId = listOps.rightPop();
                continue;
            }

            // 再新增mysql中该用户的购物车
            cartJsons.forEach(cartJson ->{
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                this.cartMapper.insert(cart);
            });

            userId = listOps.rightPop();
        }
        return ReturnT.SUCCESS;
    }
}
