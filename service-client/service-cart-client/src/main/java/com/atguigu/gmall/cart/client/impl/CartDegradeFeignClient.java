package com.atguigu.gmall.cart.client.impl;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Component
public class CartDegradeFeignClient implements CartFeignClient {


    @Override
    public Result addToCart(Long skuId, Integer skuNum, HttpServletRequest request) {
        return null;
    }

    @Override
    public Result cartList(HttpServletRequest request) {
        return null;
    }

    @Override
    public Result checkCart(Long skuId, Integer isChecked, HttpServletRequest request) {
        return null;
    }

    @Override
    public Result deleteCart(Long skuId, HttpServletRequest request) {
        return null;
    }

    @Override
    public List<CartInfo> getCartCheckedList(Long userId) {
        return null;
    }
}
