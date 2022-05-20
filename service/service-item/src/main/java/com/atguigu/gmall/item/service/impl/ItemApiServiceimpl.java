package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemApiService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class ItemApiServiceimpl implements ItemApiService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public Map<String, Object> getItemApi(Long skuId) {
        Map<String, Object> result = new HashMap<>();
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        result.put("skuInfo",skuInfo);
        if (skuInfo != null){
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            result.put("categoryView",categoryView);

            List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            result.put("spuSaleAttrList",spuSaleAttrListCheckBySku);

            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            String skuString = JSON.toJSONString(skuValueIdsMap);
            result.put("valuesSkuJson",skuString);

            List<SpuPoster> spuPosterBySpuId = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
            result.put("spuPosterList",spuPosterBySpuId);

        }
        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
        result.put("price",skuPrice);

        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        List<Map<String, Object>> skuAttrList = attrList.stream().map(baseAttrInfo -> {
            Map<String, Object> attrMap = new HashMap<>();
            String attrName = baseAttrInfo.getAttrName();
            attrMap.put("attrName", attrName);
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            attrMap.put("attrValue", attrValueList.get(0).getValueName());
            return attrMap;
        }).collect(Collectors.toList());
        result.put("skuAttrList",skuAttrList);

        return result;
    }
}
