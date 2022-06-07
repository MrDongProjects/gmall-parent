package com.atguigu.gmall.order.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderDegradeFeignClient implements OrderFeignClient {

    /**
     * 确认订单
     * @param
     * @return
     */
    @Override
    public Result<Map<String, Object>> trade() {
        return Result.fail();
    }

    /**
     * 提交订单 结算
     * @param orderInfo
     * @param
     * @return
     */
    @Override
    public Result submitOrder(OrderInfo orderInfo) {
        return Result.fail();
    }

    /**
     * 获取订单详情
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        return null;
    }

    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     * @param orderInfo
     * @return
     */
    @Override
    public Long submitOrderMs(OrderInfo orderInfo) {
        return null;
    }
}
