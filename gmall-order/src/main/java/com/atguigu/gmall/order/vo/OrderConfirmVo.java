package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    private List<UserAddressEntity> addresses; // 收货人列表

    private List<OrderItemVo> items; // 送货清单

    private Integer bounds; // 购物积分

    private String orderToken; // 防重的唯一标识
}
