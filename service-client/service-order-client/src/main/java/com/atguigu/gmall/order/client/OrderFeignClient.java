package com.atguigu.gmall.order.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(value = "service-order",fallback = OrderDegradeFeignClient.class)
public interface OrderFeignClient {

    /**
     * 确认订单
     * @param
     * @return
     */
    @GetMapping("/api/order/auth/trade")
    public Result trade();

    /**
     * 提交订单 结算
     * @param orderInfo
     * @param
     * @return
     */
    @PostMapping("/api/order/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo);

    /**
     * 内部调用获取订单
     * @param orderId
     * @return
     */
    @GetMapping("/api/order/inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId);

    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     * @param orderInfo
     * @return
     */
    @PostMapping("/api/order/inner/seckill/submitOrder")
    public Long submitOrderMs(@RequestBody  OrderInfo orderInfo);

}
