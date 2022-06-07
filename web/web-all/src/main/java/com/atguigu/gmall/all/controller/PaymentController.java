package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@SuppressWarnings("all")
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("/pay.html")
    public String PayPage(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.valueOf(orderId));
        request.setAttribute("orderInfo", orderInfo);
        return "payment/pay";
    }

    /**
     * 支付成功页
     */
    @GetMapping("pay/success.html")
    public String successPage(){
        return "payment/success";
    }
}
