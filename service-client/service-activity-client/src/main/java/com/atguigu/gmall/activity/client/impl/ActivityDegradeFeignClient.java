package com.atguigu.gmall.activity.client.impl;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.stereotype.Service;

@Service
public class ActivityDegradeFeignClient implements ActivityFeignClient {
    /**
     * 秒杀列表查询
     * @return
     */
    @Override
    public Result findAll() {
        return Result.fail();
    }

    /**
     * 根据ID获取实体
     * @param skuId
     * @return
     */
    @Override
    public Result getSeckillGoods(Long skuId) {
        return Result.fail();
    }

    /**
     * 秒杀确认订单
     * @return
     */
    @Override
    public Result trade() {
        return Result.fail();
    }
}
