package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;


@Service
@SuppressWarnings("all")
public class AlipayServiceimpl implements AlipayService {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    /**
     * 通过支付宝下单
     * @param orderId
     * @return
     */
    @Override
    public String createaliPay(Long orderId) {
        //根据orderId获取orderInfo信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //判断订单状态
        if("PAID".equals(orderInfo.getOrderStatus())||
                "CLOSED".equals(orderInfo.getOrderStatus())){
            return "该订单已经支付或者已经超时关闭！";
        }
        //添加支付记录
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());

        //调用支付宝，获取支付页面二维码

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //设置电商异步路径
        request.setNotifyUrl(AlipayConfig.notify_payment_url);
        //设置电商同步请求路径
        request.setReturnUrl(AlipayConfig.return_payment_url);

        JSONObject bizContent = new JSONObject();
        //设置交易订单编号
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        //设置订单总金额
        bizContent.put("total_amount", 0.01);
        //设置订单描述
        bizContent.put("subject", orderInfo.getTradeBody());
        //商品code,固定值
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        //设置二维码超时时间
        bizContent.put("timeout_express","10m");

        //参数设置到请求对象中
        request.setBizContent(bizContent.toString());

        AlipayTradePagePayResponse response = null;
        try {
            //执行请求支付宝
            response = alipayClient.pageExecute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            //获取支付宝返回的页面信息
            String from = response.getBody();
            //返回信息
            return from;
        } else {
            return "调用失败";
        }
    }



    /**
     * 退款
     * @param orderId
     * @return
     */
    @Override
    public boolean refund(Long orderId) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        JSONObject map = new JSONObject();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("refund_amount",0.01);
        map.put("refund_reason","null");
        request.setBizContent(map.toString());

        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()){
//            // 更新交易记录 ： 关闭
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
            paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(),PaymentType.ALIPAY.name(),paymentInfo);
            return true;
        }
        return false;
    }

    /**
     * 根据订单Id关闭订单
     * @param orderId
     * @return
     */
    @Override
    @SneakyThrows
    public Boolean closePay(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("operator_id","YX01");
        request.setBizContent(JSON.toJSONString(map));

        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }

    /**
     * 根据订单查询是否支付成功
     * @param orderId
     * @return
     */
    @Override
    @SneakyThrows
    public boolean checkPayment(Long orderId) {
        // 根据订单Id 查询订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        // 根据out_trade_no 查询交易记录
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            return true;
        } else {
            return false;
        }

    }

}
