package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();

        // 根据skuId查询sku，设置sku相关信息
        CompletableFuture<SkuEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return null;
            }
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            return skuEntity;
        }, threadPoolExecutor);

        CompletableFuture<Void> categoryCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 根据sku中的categoryId查询一二三级分类
            ResponseVo<List<CategoryEntity>> categoryResponseVo = this.pmsClient.query123CategoriesByCid3(skuEntity.getCatagoryId());
            itemVo.setCategories(categoryResponseVo.getData());
        }, threadPoolExecutor);

        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 根据sku中的brandId查询品牌
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 根据sku中的spuId查询spu
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        // 根据skuId查询sku的图片信息
        CompletableFuture<Void> imageCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> imagesResponseVo = this.pmsClient.queryImagesBySkuId(skuId);
            itemVo.setImages(imagesResponseVo.getData());
        }, threadPoolExecutor);

        CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
            // 根据skuId查询sku的营销信息
            ResponseVo<List<ItemSaleVo>> itemSalesResponseVo = this.smsClient.querySalesBySkuId(skuId);
            itemVo.setSales(itemSalesResponseVo.getData());
        }, threadPoolExecutor);

        CompletableFuture<Void> wareCompletableFuture = CompletableFuture.runAsync(() -> {
            // 根据skuId查询库存信息
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> saleAttrCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 根据sku中的spuId查询spu下所有sku的选择值集合
            ResponseVo<List<SaleAttrValueVo>> salesResponseVo = this.pmsClient.queryAllSaleAttrValueBySpuId(skuEntity.getSpuId());
            itemVo.setSaleAttrs(salesResponseVo.getData());
        }, threadPoolExecutor);

        CompletableFuture<Void> skuSaleCompletableFuture = CompletableFuture.runAsync(() -> {
            // 根据skuId查询当前sku的销售属性
            ResponseVo<List<SkuAttrValueEntity>> skuSaleAttrValueResponseVo = this.pmsClient.querySaleAttrValueBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = skuSaleAttrValueResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                Map<Long, String> saleAttr = skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue));
                itemVo.setSaleAttr(saleAttr);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> jsonCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 根据sku中spuId查询spu下所有sku销售属性组合与skuId的映射关系
            ResponseVo<String> jsonResponseVo = this.pmsClient.querySaleAttrMappingSkuIdBySpuId(skuEntity.getSpuId());
            itemVo.setSkusJson(jsonResponseVo.getData());
        }, threadPoolExecutor);

        CompletableFuture<Void> descCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 根据sku中spuId查询spu的描述信息（海报信息）
            ResponseVo<SpuDescEntity> descEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = descEntityResponseVo.getData();
            if (spuDescEntity != null) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 根据sku中的分类id、spuId以及skuId 查询组及组下的规格参数和值
            ResponseVo<List<ItemGroupVo>> groupResponseVo = this.pmsClient.queryGroupWithAttrValue(skuEntity.getCatagoryId(), skuEntity.getSpuId(), skuId);
            itemVo.setGroups(groupResponseVo.getData());
        }, threadPoolExecutor);

        CompletableFuture.allOf(categoryCompletableFuture, brandCompletableFuture, spuCompletableFuture,
                imageCompletableFuture, saleCompletableFuture, wareCompletableFuture, saleAttrCompletableFuture,
                skuSaleCompletableFuture, jsonCompletableFuture, descCompletableFuture, groupCompletableFuture).join();

        return itemVo;
    }
}
