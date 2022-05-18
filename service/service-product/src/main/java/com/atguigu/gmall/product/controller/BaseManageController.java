package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.BaseManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author Echo
 * @Create 2022-05-13-20:37
 * @Description
 */
@RestController
@RequestMapping("/admin/product")
//@CrossOrigin
public class BaseManageController {

    @Autowired
    private BaseManageService baseManageService;

    @GetMapping("/getCategory1")
    public Result<List<BaseCategory1>> getCategory1(){
        List<BaseCategory1> baseCategory1List =  baseManageService.getCategory1();
        return Result.ok(baseCategory1List);
    }

    @GetMapping("/getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){
        List<BaseCategory2> baseCategory2List = baseManageService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }

    @GetMapping("/getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id){
        List<BaseCategory3> baseCategory3List = baseManageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    @GetMapping("/attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result attrInfoList(@PathVariable Long category1Id,@PathVariable Long category2Id,@PathVariable Long category3Id){
        List<BaseAttrInfo> baseAttrInfoList = baseManageService.attrInfoList(category1Id,category2Id,category3Id);
        return Result.ok(baseAttrInfoList);
    }

    ///admin/product/saveAttrInfo
    @PostMapping("saveAttrInfo")
    public Result saveOrUpdateAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        baseManageService.saveOrUpdateAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    ///admin/product/getAttrValueList/{attrId}
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId){
        BaseAttrInfo baseAttrInfo = baseManageService.getAttrInfo(attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(baseAttrValueList);
    }

}

















