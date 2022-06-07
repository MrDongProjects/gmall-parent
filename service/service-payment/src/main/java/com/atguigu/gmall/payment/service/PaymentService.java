package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    /**
     * 保存交易记录
     * @param orderInfo
     * @param paymentType 支付类型（1：微信 2：支付宝）
     */
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    /**
     * 根据outTradeNo 查询数据！
     * @param tradeNo
     * @return
     */
    PaymentInfo getPaymentInfo(String tradeNo, String name);

    /**
     * 修改交易记录状态
     * @param tradeNo
     * @param name
     * @param paramMap
     */
    void paySuccess(String tradeNo, String name, Map<String, String> paramMap);


    /**
     * 更新订单信息
     * @param tradeNo
     * @param name
     * @param paymentInfo
     */
    void updatePaymentInfo(String tradeNo, String name, PaymentInfo paymentInfo);

    /**
     * 关闭订单
     * @param orderId
     */
    void closePayment(Long orderId);
}
