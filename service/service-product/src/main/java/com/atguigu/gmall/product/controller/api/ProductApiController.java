package com.atguigu.gmall.product.controller.api;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.BaseManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    // /api/product/inner/getSkuPrice/{skuId} 根据skuId 获取最新的商品价格
    @GetMapping("/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        return baseManageService.getSkuPrice(skuId);
    }

    // /api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}  根据spuId,skuId 获取销售属性数据
    @GetMapping("/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId") Long skuId,@PathVariable("spuId") Long spuId){
        return baseManageService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    // /api/product/inner/getSkuValueIdsMap/{spuId} 根据spuId 获取到销售属性值Id 与skuId 组成的数据集
    @GetMapping("/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId){
        return baseManageService.getSkuValueIdsMap(spuId);
    }

    // /api/product/inner/findSpuPosterBySpuId/{spuId}  根据spuId 获取海报数据
    @GetMapping("/findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> findSpuPosterBySpuId(@PathVariable Long spuId){
        return baseManageService.findSpuPosterBySpuId(spuId);
    }

    // /api/product/inner/getAttrList/{skuId} 根据skuId 获取平台属性数据
    @GetMapping("/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
        return baseManageService.getAttrList(skuId);
    }


}
