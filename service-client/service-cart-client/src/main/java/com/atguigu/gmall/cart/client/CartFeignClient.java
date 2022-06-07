package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.cart.client.impl.CartDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@FeignClient(value = "service-cart",fallback = CartDegradeFeignClient.class)
public interface CartFeignClient {

    /**
     * 添加购物车
     * @param skuId
     * @param skuNum
     * @param request
     * @return
     */
    @GetMapping("/api/cart/addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum, HttpServletRequest request);

    /**
     * 通过用户Id 查询购物车列表
     * @param request
     * @return
     */
    @GetMapping("/api/cart/cartList")
    public Result cartList(HttpServletRequest request);

    /**
     * 更新选中状态
     * @param skuId
     * @param isChecked
     * @param request
     * @return
     */
    @GetMapping("/api/cart/checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,@PathVariable Integer isChecked,HttpServletRequest request);

    /**
     * 删除购物车
     * @param skuId
     * @return
     */
    @DeleteMapping("/api/cart/deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,HttpServletRequest request);

    /**
     * 获取选中状态的购物车列表
     * @param userId
     * @return
     */
    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable Long userId);
}
