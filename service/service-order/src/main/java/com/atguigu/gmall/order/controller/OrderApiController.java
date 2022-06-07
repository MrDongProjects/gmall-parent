package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/api/order")
@SuppressWarnings("all")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 确认订单
     * @param request
     * @return
     */
    @GetMapping("/auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //获取用户地址
        List<UserAddress> userAddressListByUserId = userFeignClient.findUserAddressListByUserId(Long.valueOf(userId));
        // 先得到用户想要购买的商品！
        List<CartInfo> cartInfoList = cartFeignClient.getCartCheckedList(Long.valueOf(userId));
        // 声明一个集合来存储订单明细
        List<OrderDetail> detailArrayList = new ArrayList<>();
        int totalNum = 0;
        for (CartInfo cartInfo : cartInfoList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            detailArrayList.add(orderDetail);
            //计算个数
            totalNum+=cartInfo.getSkuNum();
        }
        //计算总金额
        OrderInfo orderInfo = new OrderInfo();

        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();
        Map<String, Object> map = new HashMap<>();
        // 获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        map.put("tradeNo", tradeNo);
        map.put("userAddressList", userAddressListByUserId);
        map.put("detailArrayList", detailArrayList);
        // 保存总金额
        map.put("totalNum", detailArrayList.size());
        map.put("totalAmount", orderInfo.getTotalAmount());


        return Result.ok(map);
    }

    /**
     * 提交订单 结算
     * @param orderInfo
     * @param request
     * @return
     */
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setId(Long.valueOf(userId));

        // 获取前台页面的流水号
        String tradeNo = request.getParameter("tradeNo");
        // 调用服务层的比较方法
        boolean flag = orderService.checkTradeCode(userId,tradeNo);
        if (!flag){
            // 比较失败！
            return Result.fail().message("不能重复提交订单！");
        }
        //删除流水号
        orderService.deleteTradeNo(userId);

        List<String> errorList = new ArrayList<>();
        List<CompletableFuture> futureList = new ArrayList<>();
        //验证库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            CompletableFuture<Void> checkStockCompletableFuture = CompletableFuture.runAsync(() -> {
                //验证库存
                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!result) {
                    errorList.add(orderDetail.getSkuName() + "库存不足!");
                }
            }, threadPoolExecutor);
            futureList.add(checkStockCompletableFuture );


            CompletableFuture<Void> checkPriceCompletableFuture = CompletableFuture.runAsync(() -> {
                //验证价格:
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                    //重新查询价格
                    //设置最新的价格
                    List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(Long.valueOf(userId));
                    //写入缓存
                    cartCheckedList.forEach(cartInfo -> {
                        redisTemplate.opsForHash().put(RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX, cartInfo.getUserId().toString(), cartInfo);
                    });
                    errorList.add(orderDetail.getSkuName() + "价格有变动!");
                }
            }, threadPoolExecutor);
            futureList.add(checkPriceCompletableFuture);
            //合并线程
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
            if (errorList.size() > 0){
                return Result.fail().message(StringUtils.join(errorList,","));
            }
        }
        // 验证通过，保存订单！
        Long orderId = orderService.submitOrder(orderInfo);
        return Result.ok(orderId);
    }

    /**
     * 我的订单
     * @param page
     * @param limit
     * @param request
     * @return
     */
    @GetMapping("/auth/{page}/{limit}")
    public Result pageList(@PathVariable Long page,@PathVariable Long limit,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        Page<OrderInfo> orderInfoPage =new Page<>(page,limit);
        IPage<OrderInfo> pageModel = orderService.getPage(orderInfoPage,userId);
        return Result.ok(pageModel);
    }

    /**
     * 内部调用获取订单
     * @param orderId
     * @return
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){
        return orderService.getOrderInfo(orderId);
    }

    /**
     * 拆单业务
     * @param request
     * @return
     */
    @RequestMapping("/orderSplit")
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        // 拆单：获取到的子订单集合
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId),wareSkuMap);
        // 声明一个存储map的集合
        ArrayList<Map> mapArrayList = new ArrayList<>();
        // 生成子订单集合
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderService.initWareOrder(orderInfo);
            // 添加到集合中！
            mapArrayList.add(map);
        }
        return JSON.toJSONString(mapArrayList);
    }

    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     * @param orderInfo
     * @return
     */
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrderMs(@RequestBody  OrderInfo orderInfo){
        Long orderId = orderService.submitOrder(orderInfo);
        return orderId;
    }

}
