package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.SkuManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class SkuManageController {

    @Autowired
    private SkuManageService skuManageService;

    // /admin/product/spuImageList/{spuId} 根据spuId 获取spuImage 集合
    @GetMapping("/spuImageList/{spuId}")
    public Result spuImageList(@PathVariable Long spuId){
        List<SpuImage> spuImageList = skuManageService.spuImageList(spuId);

        return Result.ok(spuImageList);
    }

    // /admin/product/spuSaleAttrList/{spuId} 根据spuId 查询销售属性
    @GetMapping("/spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId){
        List<SpuSaleAttr> spuSaleAttrList = skuManageService.spuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrList);
    }

    // /admin/product/saveSkuInfo 保存SkuInfo
    @PostMapping("/saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        skuManageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    // /admin/product/onSale/{skuId} 上架
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        skuManageService.onSale(skuId);
        return  Result.ok();
    }

    // /admin/product/cancelSale/{skuId} 下架
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        skuManageService.cancelSale(skuId);
        return Result.ok();
    }

    // /admin/product/list/{page}/{limit} sku分页列表
    @GetMapping("/list/{page}/{limit}")
    public Result skuList(@PathVariable Long page,@PathVariable Long limit){
        Page<SkuInfo> skuInfoPage = new Page<>(page,limit);
        IPage<SkuInfo> skuInfoIPage = skuManageService.getPage(skuInfoPage);
        return Result.ok(skuInfoIPage);
    }


}
