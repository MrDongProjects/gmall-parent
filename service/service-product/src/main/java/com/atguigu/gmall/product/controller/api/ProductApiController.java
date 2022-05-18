package com.atguigu.gmall.product.controller.api;

import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.BaseManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product/inner")
public class ProductApiController {

    @Autowired
    private BaseManageService baseManageService;

    // /api/product/inner/getSkuInfo/{skuId}  根据skuId获取SkuInfo
    @GetMapping("/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId){
        return baseManageService.getSkuInfo(skuId);
    }

    // /api/product/inner/getCategoryView/{category3Id} 根据三级分类id获取分类信息
    @GetMapping("/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        return baseManageService.getCategoryView(category3Id);
    }


}
