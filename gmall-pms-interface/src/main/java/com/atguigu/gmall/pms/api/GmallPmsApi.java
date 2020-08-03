package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {

    /**
     * 列表
     */
    @PostMapping("pms/spu/json")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkusBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/skuattrvalue/search/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/spuattrvalue/search/{spuId}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValueBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> queryImagesBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByPid(@PathVariable("parentId")Long pid);

    @GetMapping("pms/category/cates/{pid}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesWithSubByPid(@PathVariable("pid")Long pid);

    @GetMapping("pms/category/all/{cid}")
    public ResponseVo<List<CategoryEntity>> query123CategoriesByCid3(@PathVariable("cid")Long cid);

    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> queryAllSaleAttrValueBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySaleAttrValueBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/skuattrvalue/sku/spu/{spuId}")
    public ResponseVo<String> querySaleAttrMappingSkuIdBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/attrgroup/withattr/withvalue/{cid}")
    public ResponseVo<List<ItemGroupVo>> queryGroupWithAttrValue(@PathVariable("cid")Long cid,
                                                                 @RequestParam("spuId") Long spuId,
                                                                 @RequestParam("skuId")Long skuId);
}
