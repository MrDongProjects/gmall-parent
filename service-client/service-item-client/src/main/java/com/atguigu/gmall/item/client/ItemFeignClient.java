package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(value = "service-item",fallback = ItemDegradeFeignClient.class)
@Repository
public interface ItemFeignClient {

    /**
     * 详情接口
     * @param skuId
     * @return
     */
    @GetMapping("/api/item/{skuId}")
    public Result itemApi(@PathVariable Long skuId);

}
