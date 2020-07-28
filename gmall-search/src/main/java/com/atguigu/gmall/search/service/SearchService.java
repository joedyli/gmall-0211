package com.atguigu.gmall.search.service;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SearchResponseVo search(SearchParamVo paramVo) {
        try {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("goods");
            searchRequest.source(buildDSL(paramVo));
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(response.toString());
            SearchResponseVo responseVo = this.parseResult(response);
            // 分页信息
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SearchSourceBuilder buildDSL(SearchParamVo paramVo){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 1. 构建关键字查询
        String keyword = paramVo.getKeyword();
        if (StringUtils.isBlank(keyword)){
            // 打广告
            return null;
        }
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));

        // 2. 构建条件过滤
        // 2.1. 品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }

        // 2.2. 分类过滤
        List<Long> cid = paramVo.getCid();
        if (!CollectionUtils.isEmpty(cid)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", cid));
        }

        // 2.3. 价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if (priceFrom != null || priceTo != null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            boolQueryBuilder.filter(rangeQuery);
            if (priceFrom != null){
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null){
                rangeQuery.lte(priceTo);
            }
        }

        // 2.4. 库存过滤
        Boolean store = paramVo.getStore();
        if (store != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }

        // 2.5. 规格参数过滤：["8:8G-12G", "9:128G-256G"]
        List<String> props = paramVo.getProps();
        if (!CollectionUtils.isEmpty(props)){
            props.forEach(prop -> { // 每一个prop:  8:8G-12G
                // 先以：分割获取规格参数id 以及8G-12G
                String[] attr = StringUtils.split(prop, ":");
                if (attr != null && attr.length == 2){
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attr[0]));
                    // 再以-分割获取规格参数值数组
                    String[] attrValues = StringUtils.split(attr[1], "-");
                    if (attrValues != null && attrValues.length > 0){
                        boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));
                    }
                    NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None);
                    boolQueryBuilder.filter(nestedQuery);
                }
            });
        }

        // 3. 排序
        Integer sort = paramVo.getSort();
        if (sort != null) { // 1-价格升序 2-价格降序 3-销量降序 4-新品排序
            switch (sort) {
                case 1: sourceBuilder.sort("price", SortOrder.ASC); break;
                case 2: sourceBuilder.sort("price", SortOrder.DESC); break;
                case 3: sourceBuilder.sort("sale", SortOrder.DESC); break;
                case 4: sourceBuilder.sort("createTime", SortOrder.DESC); break;
            }
        }

        // 4. 分页
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);

        // 5. 高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style='color:red'>").postTags("</font>"));

        // 6. 聚合
        // 6.1. 品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoAgg").field("logo"))
        );

        // 6.2. 分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"))
        );

        // 6.3. 规格参数聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))
                )
        );

        // 7.指定包含的字段
        sourceBuilder.fetchSource(new String[]{"skuId", "title", "price", "image", "subTitle"}, null);

        System.out.println(sourceBuilder.toString());
        return sourceBuilder;
    }

    private SearchResponseVo parseResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();

        // 总记录数
        SearchHits hits = response.getHits();
        responseVo.setTotal(hits.totalHits);

        // 获取当前页的数据
        SearchHit[] hitsHits = hits.getHits();
        if (hitsHits == null || hitsHits.length == 0){
            return responseVo;
        }
        List<Goods> goodsList = Stream.of(hitsHits).map(hit -> {
            try {
                // 获取命中结果集中的_source
                String source = hit.getSourceAsString();
                // 把_source反序列化为Goods对象
                Goods goods = MAPPER.readValue(source, Goods.class);

                // 把_source中的普通的Title 替换成 高亮结果集中title
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                if (!CollectionUtils.isEmpty(highlightFields)) {
                    HighlightField highlightField = highlightFields.get("title");
                    if (highlightField != null) {
                        Text[] fragments = highlightField.getFragments();
                        if (fragments != null && fragments.length > 0){
                            String title = fragments[0].string();
                            goods.setTitle(title);
                        }
                    }
                }
                return goods;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        // 获取聚合
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();

        // 解析品牌id聚合获取品牌的过滤条件
        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        List<BrandEntity> brandEntities = buckets.stream().map(bucket -> {
            BrandEntity brandEntity = new BrandEntity();

            // 解析出桶中的品牌id
            Long brandId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
            brandEntity.setId(brandId);
            // 获取桶中的子聚合
            Map<String, Aggregation> subAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
            // 获取品牌名称的子聚合
            ParsedStringTerms brandNameAgg = (ParsedStringTerms)subAggregationMap.get("brandNameAgg");
            List<? extends Terms.Bucket> nameBuckets = brandNameAgg.getBuckets();
            if (!CollectionUtils.isEmpty(nameBuckets)){
                Terms.Bucket nameBucket = nameBuckets.get(0);
                if (nameBucket != null) {
                    brandEntity.setName(nameBucket.getKeyAsString());
                }
            }

            // 获取logo的子聚合
            ParsedStringTerms brandLogoAgg = (ParsedStringTerms)subAggregationMap.get("brandLogoAgg");
            if (brandLogoAgg != null) {
                List<? extends Terms.Bucket> logoBuckets = brandLogoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoBuckets)){
                    Terms.Bucket logoBucket = logoBuckets.get(0);
                    if (logoBucket != null) {
                        brandEntity.setLogo(logoBucket.getKeyAsString());
                    }
                }
            }
            return brandEntity;
        }).collect(Collectors.toList());
        responseVo.setBrands(brandEntities);

        // 解析分类id聚合获取分类的过滤条件
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        List<CategoryEntity> categoryEntities = categoryBuckets.stream().map(bucket -> {
            CategoryEntity categoryEntity = new CategoryEntity();

            Long categoryId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
            categoryEntity.setId(categoryId);

            ParsedStringTerms categoryNameAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
            if (categoryNameAgg != null) {
                List<? extends Terms.Bucket> nameAggBuckets = categoryNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)){
                    categoryEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }
            }

            return categoryEntity;
        }).collect(Collectors.toList());
        responseVo.setCategories(categoryEntities);

        // 获取规格参数的聚合
        ParsedNested attrAgg = (ParsedNested)aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms)attrAgg.getAggregations().get("attrIdAgg");
        if (attrIdAgg != null){
            List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
            if (!CollectionUtils.isEmpty(attrIdAggBuckets)){
                List<SearchResponseAttrVo> searchResponseAttrVos = attrIdAggBuckets.stream().map(bucket -> {
                    SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();

                    // 解析桶中的key获取规格参数的id
                    Long attrId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
                    searchResponseAttrVo.setAttrId(attrId);

                    // 获取该参数所有的子聚合
                    Map<String, Aggregation> subAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                    // 获取规格参数名的子聚合
                    ParsedStringTerms attrNameAgg = (ParsedStringTerms)subAggregationMap.get("attrNameAgg");
                    if (attrNameAgg != null) {
                        List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                        if (!CollectionUtils.isEmpty(nameAggBuckets)){
                            Terms.Bucket nameBucket = nameAggBuckets.get(0);
                            if (nameBucket != null) {
                                searchResponseAttrVo.setAttrName(nameBucket.getKeyAsString());
                            }
                        }
                    }

                    // 获取规格参数值子聚合
                    ParsedStringTerms attrValueAgg = (ParsedStringTerms)subAggregationMap.get("attrValueAgg");
                    if (attrValueAgg != null) {
                        List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                        if (!CollectionUtils.isEmpty(attrValueAggBuckets)){
                            searchResponseAttrVo.setAttrValues(attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                        }
                    }

                    return searchResponseAttrVo;
                }).collect(Collectors.toList());
                responseVo.setAttrs(searchResponseAttrVos);
            }
        }

        return responseVo;
    }
}
