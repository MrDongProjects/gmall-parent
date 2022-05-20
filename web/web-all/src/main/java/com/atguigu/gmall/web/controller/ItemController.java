package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class ItemController {
    @Autowired
    private ItemFeignClient itemFeignClient;

    /**
     * sku详情页面
     * @param skuId
     * @param model
     * @return
     */

    @RequestMapping("/{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model){
        Result<Map> result = itemFeignClient.itemApi(skuId);
        model.addAllAttributes(result.getData());
        return  "item/item";
    }

}
