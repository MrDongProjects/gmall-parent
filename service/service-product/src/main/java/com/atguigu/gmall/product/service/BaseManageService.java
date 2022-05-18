package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.*;

import java.util.List;

/**
 * @Author Echo
 * @Create 2022-05-13-20:42
 * @Description
 */
public interface BaseManageService {

    List<BaseCategory1> getCategory1();

    List<BaseCategory2> getCategory2(Long category1Id);

    List<BaseCategory3> getCategory3(Long category2Id);

    List<BaseAttrInfo> attrInfoList(Long category1Id, Long category2Id, Long category3Id);

    void saveOrUpdateAttrInfo(BaseAttrInfo baseAttrInfo);


    BaseAttrInfo getAttrInfo(Long attrId);

    SkuInfo getSkuInfo(Long skuId);

    BaseCategoryView getCategoryView(Long category3Id);
}
