package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/product/baseTrademark/")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    //分页列表 /admin/product/baseTrademark/{page}/{limit}
    @GetMapping("{page}/{limit}")
    public Result getTrademarkPage(@PathVariable Long page, @PathVariable Long limit){
        Page<BaseTrademark> baseTrademarkPage = new Page<>(page,limit);
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.getTrademarkPage(baseTrademarkPage);
        return Result.ok(baseTrademarkIPage);
    }

    //根据id查询详情 /admin/product/baseTrademark/get/{id}
    @GetMapping("get/{id}")
    public Result getTrademark(@PathVariable Long id){
        baseTrademarkService.getById(id);
        return Result.ok();
    }

    //保存信息 /admin/product/baseTrademark/save
    @PostMapping("/save")
    public Result saveTrademark(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    //修改 /admin/product/baseTrademark/update
    @PutMapping("/update")
    public Result updateTrademark(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    //删除 /admin/product/baseTrademark/remove/{id}
    @DeleteMapping("/remove/{id}")
    public Result removeTrademark(@PathVariable Long id){
        baseTrademarkService.removeById(id);
        return Result.ok();
    }


}
