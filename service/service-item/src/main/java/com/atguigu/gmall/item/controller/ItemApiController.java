package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/item")
public class ItemApiController {

    @Autowired
    private ItemApiService itemApiService;

    // /api/item/{skuId}   详情接口
    @GetMapping("/{skuId}")
    public Result itemApi(@PathVariable Long skuId){
        Map<String, Object> result = itemApiService.getItemApi(skuId);
        return Result.ok(result);
    }

}
