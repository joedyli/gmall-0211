package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    // 面包屑：三级分类
    private List<CategoryEntity> categories;

    // 面包屑：品牌信息
    private Long brandId;
    private String brandName;

    // 面包屑：spu信息
    private Long spuId;
    private String spuName;

    // 中间核心部分
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private Integer weight;
    private String defaultImage;

    // sku图片列表
    private List<SkuImagesEntity> images;

    // sku营销信息
    private List<ItemSaleVo> sales;

    // 是否有货
    private Boolean store = false;

    // 用户选择销售属性集合
    // [{attrId: 4, attrName: 内存, attrValues: [8G, 12G]}
    //  {attrId: 5, attrName: 存储, attrValues: [128G, 256G]}
    // {attrId: 6, attrName: 颜色, attrValues: [白天白, 暗夜黑]}]
    private List<SaleAttrValueVo> saleAttrs;

    // 当前sku的销售属性：{4: 8G, 5: 128G, 6: 暗夜黑}
    private Map<Long, String> saleAttr;

    // 当前sku所属spu下，所有sku可能的组合
    // {'8G,128G,暗夜黑': 100, '8G,128G,白天白': 101, '8G,256G,暗夜黑': 102}
    private String skusJson;

    // 商品描述
    private List<String> spuImages;

    // 规格参数组及组下的规格参数与值
    private List<ItemGroupVo> groups;
}
