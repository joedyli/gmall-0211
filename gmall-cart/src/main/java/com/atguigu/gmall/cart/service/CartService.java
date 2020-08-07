package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.Interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.config.CartAsyncExceptionHandler;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartAsyncService cartAsyncService;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:info:";

    public void saveCart(Cart cart) {

        // 获取用户的登录信息
        String userId = null;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getUserId() != null){
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX + userId;

        // 获取内层map的操作对象（该用户所有购物车的集合）{8: cart, 9:cart}
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        // 判断该用户购物车中是否已有该商品
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)) {
            // 有，更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount().add(count));

            // 重新放入redis和mysql数据库
            //this.cartMapper.updateCartByUserIdAndSkuId(userId, cart);
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId, cart);
        } else {
            // 无，给该用户新增一条记录
            cart.setUserId(userId);
            cart.setCheck(true);

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null){
                cart.setTitle(skuEntity.getTitle());
                cart.setPrice(skuEntity.getPrice());
                cart.setDefaultImage(skuEntity.getDefaultImage());
            }

            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));

            ResponseVo<List<ItemSaleVo>> listResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = listResponseVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));

            // 新增mysql数据库
            this.cartAsyncService.addCart(cart);
        }
        // redis中，更新和新增都是put方法
        hashOps.put(skuId, JSON.toJSONString(cart));
    }

    public Cart queryCartBySkuId(Long skuId) {

        String key = KEY_PREFIX;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getUserId() != null){
            key += userInfo.getUserId();
        } else {
            key += userInfo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        if (hashOps.hasKey(skuId.toString())){
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson, Cart.class);
        } else {
            throw new RuntimeException("该用户不存在对应商品的购物车记录！");
        }
    }

    public List<Cart> queryCarts() {
        // 1.查询未登录的购物车
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unLoginKey = KEY_PREFIX + userKey;
        BoundHashOperations<String, Object, Object> unLoginHashOps = this.redisTemplate.boundHashOps(unLoginKey);
        // 获取未登录的购物车集合
        List<Object> unLoginCartJsons = unLoginHashOps.values();
        List<Cart> unLoginCarts = null;
        if(!CollectionUtils.isEmpty(unLoginCartJsons)){
            unLoginCarts = unLoginCartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(), Cart.class)).collect(Collectors.toList());
        }

        // 2.判断登录状态
        Long userId = userInfo.getUserId();
        if (userId == null){
            // 3.未登录，返回未登录的购物车
            return unLoginCarts;
        }

        // 4.登录，合并未登录的购物车到登录状态的购物车
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                BigDecimal count = cart.getCount(); // 未登录购物车的数量
                if (loginHashOps.hasKey(cart.getSkuId().toString())){
                    String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                    cart = JSON.parseObject(cartJson, Cart.class); // 登录情况下的购物车
                    cart.setCount(cart.getCount().add(count));
                    // 更新redis和mysql中数量
                    this.cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(), cart);
                } else {
                    cart.setUserId(userId.toString());
                    this.cartAsyncService.addCart(cart);
                }
                loginHashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            });
        }

        // 5.删除未登录的购物车
        this.cartAsyncService.deleteCartsByUserId(userKey);
        this.redisTemplate.delete(unLoginKey);

        // 6.查询登录状态的购物车
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)){
            return loginCartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(), Cart.class)).collect(Collectors.toList());
        }
        return null;
    }

    public void updateNum(Cart cart) {

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if (userInfo.getUserId() != null){
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);

            this.cartAsyncService.updateCartByUserIdAndSkuId(userId, cart);
            hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
        }
    }

    public void deleteCart(Long skuId) {

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if (userInfo.getUserId() != null){
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        if (hashOps.hasKey(skuId.toString())){
            this.cartAsyncService.deleteCartByUserIdAndSkuId(userId, skuId);
            hashOps.delete(skuId.toString());
        }
    }

    @Async
    public String executor1(){
        try {
            System.out.println("这是service中executor1方法开始执行。。。。");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("这是service中executor1方法执行完成。。。。");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 通过AsyncResult返回方法的返回结果集
        return "hello executor1";
    }

    @Async
    public String executor2(){
        try {
            System.out.println("这是service中executor2方法开始执行。。。。");
            TimeUnit.SECONDS.sleep(5);
//            int i = 1/0;
            System.out.println("这是service中executor2方法执行完成。。。。");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "hello executor2";
    }

    @Async
    public ListenableFuture<String> executor3(){
        try {
            System.out.println("这是service中executor1方法开始执行。。。。");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("这是service中executor1方法执行完成。。。。");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 通过AsyncResult返回方法的返回结果集
        return AsyncResult.forValue("hello executor1");
    }

    @Async
    public ListenableFuture<String> executor4(){
        try {
            System.out.println("这是service中executor2方法开始执行。。。。");
            TimeUnit.SECONDS.sleep(5);
            int i = 1/0;
            System.out.println("这是service中executor2方法执行完成。。。。");
        } catch (Exception e) {
            e.printStackTrace();
            return AsyncResult.forExecutionException(e);
        }
        return AsyncResult.forValue("hello executor2");
    }


}
