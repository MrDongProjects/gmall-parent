package com.atguigu.gmall.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {

    public final static String FORMAT = "json";
    public final static String CHARSET = "utf-8";
    public final static String SIGN_TYPE = "RSA2";

    //阿里公钥
    public static String alipay_public_key;

    @Value("${alipay_public_key}")
    public void setAlipay_public_key(String alipay_public_key) {
        this.alipay_public_key = alipay_public_key;
    }

    //阿里支付网关
    @Value("${alipay_url}")
    private String alipay_url;

    //应用appId
    public static String app_id;
    @Value("${app_id}")
    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }

    //应用私钥
    @Value("${app_private_key}")
    private String app_private_key;

    //异步返回地址
    public static String notify_payment_url;
    @Value("${notify_payment_url}")
    public void setNotify_payment_url(String notify_payment_url) {
        this.notify_payment_url = notify_payment_url;
    }

    //同步成功页面显示路径
    public static String return_order_url;
    @Value("${return_order_url}")
    public void setReturn_order_url(String return_order_url) {

        this.return_order_url = return_order_url;
    }

    //同步返回地址

    public static String return_payment_url;
    @Value("${return_payment_url}")
    public void setReturn_payment_url(String return_payment_url) {

        this.return_payment_url = return_payment_url;
    }

    @Bean
    public AlipayClient alipayClient() {
        AlipayClient alipayClient = new DefaultAlipayClient(
                alipay_url,
                app_id,
                app_private_key,
                FORMAT,
                CHARSET,
                alipay_public_key,
                SIGN_TYPE);

        return alipayClient;
    }


}

