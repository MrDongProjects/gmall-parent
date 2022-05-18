package com.atguigu.gmall.product.controller;


import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.SpuManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequestMapping("/admin/product")
@RestController
public class SpuManageController {

    @Autowired
    private SpuManageService spuManageService;

    // /admin/product/{page}/{limit} 分页
    @GetMapping("/{page}/{limit}")
    public Result getSpuInfoPage(@PathVariable Long page, @PathVariable Long limit, SpuInfo spuInfo){
        Page<SpuInfo> spuInfoPage = new Page<>(page,limit);
        IPage<SpuInfo> spuInfoList = spuManageService.getSpuInfoPage(spuInfoPage,spuInfo);
        return Result.ok(spuInfoList);

    }

    // /admin/product/baseSaleAttrList 获取销售属性数据
    @GetMapping("/baseSaleAttrList")
    public Result baseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrList = spuManageService.baseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    // /admin/product/saveSpuInfo 保存spu
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        spuManageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }



}



















