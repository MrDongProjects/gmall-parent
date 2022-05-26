package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemApiService;
import com.atguigu.gmall.list.service.ListFeignClient;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class ItemApiServiceimpl implements ItemApiService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map<String, Object> getItemApi(Long skuId) {
        Map<String, Object> result = new HashMap<>();
        /**
         * 通过skuId 查询skuInfo
         */
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(new Supplier<SkuInfo>() {
            @Override
            public SkuInfo get() {
                SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
                result.put("skuInfo",skuInfo);
                return skuInfo;
            }
        }, threadPoolExecutor);

        /**
         * 查询实时价格
         */
        CompletableFuture<Void> skuPriceFuture = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
                result.put("price",skuPrice);
            }
        },threadPoolExecutor);

        /**
         * 查询sku平台属性
         */
        CompletableFuture<Void> attrListFuture = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
                List<Map<String, String>> skuAttrList = attrList.stream().map(baseAttrInfo -> {
                    Map<String, String> attrMap = new HashMap<>();
                    String attrName = baseAttrInfo.getAttrName();
                    attrMap.put("attrName", attrName);
                    List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                    attrMap.put("attrValue", attrValueList.get(0).getValueName());
                    return attrMap;
                }).collect(Collectors.toList());
                result.put("skuAttrList",skuAttrList);
            }
        },threadPoolExecutor);

        /**
         * 更新商品incrHotScore 热点
         */
        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(()->{
            listFeignClient.incrHotScore(skuId);
        },threadPoolExecutor);

        /**
         * 查询海报列表
         */
        CompletableFuture<Void> spuPosterFuture = skuInfoCompletableFuture.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                List<SpuPoster> spuPosterBySpuId = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
                result.put("spuPosterList",spuPosterBySpuId);
            }
        },threadPoolExecutor);

        /**
         * 三级分类列表
         */
        CompletableFuture<Void> categoryFuture = skuInfoCompletableFuture.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
                result.put("categoryView",categoryView);
            }
        },threadPoolExecutor);


        /**
         * 商品切换
         */
        CompletableFuture<Void> skuValueFuture = skuInfoCompletableFuture.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
                String skuString = JSON.toJSONString(skuValueIdsMap);
                result.put("valuesSkuJson",skuString);
            }
        },threadPoolExecutor);

        /**
         * 获取销售属性列表和选中关系
         */
        CompletableFuture<Void> spuSaleFuture = skuInfoCompletableFuture.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
                result.put("spuSaleAttrList",spuSaleAttrListCheckBySku);
            }
        },threadPoolExecutor);

        //多任务组合
        CompletableFuture.allOf(skuInfoCompletableFuture,
                skuPriceFuture,
                attrListFuture,
                spuPosterFuture,
                categoryFuture,
                skuValueFuture,
                spuSaleFuture,
                incrHotScoreCompletableFuture
        ).join();

        return result;

    }

}
