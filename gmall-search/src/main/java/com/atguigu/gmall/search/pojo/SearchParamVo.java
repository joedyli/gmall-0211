package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * ?keyword=手机&cid=225,250&brandId=2,3&props=8:8G-12G&props=9:128G-256G&sort=1&priceFrom=1000&priceTo=2000&pageNum=1&store=true
 */
@Data
public class SearchParamVo {

    private String keyword;
    private List<Long> brandId;
    private List<Long> cid;
    private List<String> props;

    private Integer sort; // 1-价格升序 2-价格降序 3-销量降序 4-新品排序

    private Double priceFrom;
    private Double priceTo;

    private Integer pageNum = 1;
    private final Integer pageSize = 20;

    private Boolean store;
}
