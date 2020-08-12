package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.mapper.OrderItemMapper;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {


    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    //@Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId) {

        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("该订单没有选中商品！");
        }

        // 1.新增订单表
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(userId);
        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());
        // 根据用户的id查询用户信息
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        orderEntity.setUsername(userEntity.getUsername());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setPayAmount(submitVo.getTotalPrice().add(submitVo.getPostFee()==null ? new BigDecimal(0) : submitVo.getPostFee()).subtract(new BigDecimal(submitVo.getBounds() / 100)));
        orderEntity.setFreightAmount(submitVo.getPostFee());
        // TODO：查询营销信息，计算优化金额
        orderEntity.setPromotionAmount(null);

        orderEntity.setIntegrationAmount(new BigDecimal(submitVo.getBounds() / 100));
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(1);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());
        orderEntity.setAutoConfirmDay(15);

        // TODO: 查询sms中每个商品赠送的积分进行汇总

        UserAddressEntity address = submitVo.getAddress();
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverAddress(address.getAddress());
        orderEntity.setReceiverName(address.getName());

        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(submitVo.getBounds());

        this.save(orderEntity);

        // 2.新增订单详情表
        List<OrderItemEntity> orderItemEntities = items.stream().map(item -> {
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setOrderId(orderEntity.getId());
            itemEntity.setOrderSn(submitVo.getOrderToken());

            // 根据skuId查询sku
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            itemEntity.setSkuId(item.getSkuId());
            itemEntity.setCategoryId(skuEntity.getCatagoryId());
            itemEntity.setSkuName(skuEntity.getName());
            itemEntity.setSkuPic(skuEntity.getDefaultImage());
            itemEntity.setSkuPrice(skuEntity.getPrice());
            itemEntity.setSkuQuantity(item.getCount().intValue());
            ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = this.pmsClient.querySaleAttrValueBySkuId(item.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrResponseVo.getData();
            itemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));

            // 根据sku中的spuId查询spu
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            itemEntity.setSpuId(spuEntity.getId());
            itemEntity.setSpuName(spuEntity.getName());
            // 根据品牌id查询品牌
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            itemEntity.setSpuBrand(brandEntity.getName());
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            itemEntity.setSpuPic(spuDescEntity.getDecript());
            return itemEntity;
        }).collect(Collectors.toList());
        this.orderItemService.saveBatch(orderItemEntities);
        orderEntity.setItems(orderItemEntities);

        this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "order.ttl", submitVo.getOrderToken());
        return orderEntity;
    }

}
