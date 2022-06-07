package com.atguigu.gmall.payment.service;

public interface AlipayService {
    /**
     * 通过支付宝下单
     * @param orderId
     * @return
     */
    String createaliPay(Long orderId);

    /**
     * 退款
     * @param orderId
     * @return
     */
    boolean refund(Long orderId);

    /**
     * 根据订单Id关闭订单
     * @param orderId
     * @return
     */
    Boolean closePay(Long orderId);

    /**
     * 根据订单查询是否支付成功
     * @param orderId
     * @return
     */
    boolean checkPayment(Long orderId);
}
