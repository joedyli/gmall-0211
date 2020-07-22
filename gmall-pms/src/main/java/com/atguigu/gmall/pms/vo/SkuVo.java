package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuVo extends SkuEntity {

    // sku图片的接收
    private List<String> images;

    // 积分相关信息的接收
    private BigDecimal growBounds;
    private BigDecimal buyBounds;
    private List<Integer> work;

    // 打折相关信息的接收
    private Integer fullCount;
    private BigDecimal discount;
    private Integer ladderAddOther;

    // 满减相关信息的接收
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer fullAddOther;

    private List<SkuAttrValueEntity> saleAttrs;
}
