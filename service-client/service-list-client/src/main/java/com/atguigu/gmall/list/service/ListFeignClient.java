package com.atguigu.gmall.list.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.impl.ListDegradeFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-list", fallback = ListDegradeFeignClient.class)
public interface ListFeignClient {

    /**
     * 上架
     * @param skuId
     * @return
     */
    @GetMapping("api/list/inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId);

    /**
     * 下架
     * @param skuId
     * @return
     */
    @GetMapping("api/list/inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId);

    /**
     * 更新商品的热度排名
     * @param skuId
     * @return
     */
    @GetMapping("api/list/inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId);

    /**
     * 商品搜索
     * @param searchParam
     * @return
     */
    @PostMapping("/api/list")
    public Result list(@RequestBody SearchParam searchParam);

}
