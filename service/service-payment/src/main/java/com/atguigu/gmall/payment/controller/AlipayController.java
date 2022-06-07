package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;



@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 通过支付宝下单
     * @param orderId
     * @return
     */
    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String submitOrder(@PathVariable Long orderId){
        String from = alipayService.createaliPay(orderId);
        return from;
    }

    /**
     * 支付宝回调
     * @return
     */
    @RequestMapping("/callback/return")
    public String callBack(){
        return "redirect:" + AlipayConfig.return_order_url;
    }

    /**
     * 回调(支付之后)
     * @param paramsMap
     * @return
     */
    @PostMapping("/callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String, String> paramsMap) {
        System.out.println("异步通知回来了。。。");
        //异步通知验签
        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.CHARSET, AlipayConfig.SIGN_TYPE);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //获取参数
        //订单交易号（商城）
        String out_trade_no = paramsMap.get("out_trade_no");
        //总金额
        String totalAmount = paramsMap.get("total_amount");
        //appid
        String app_id = paramsMap.get("app_id");
        //获取支付状态
        String tradeStatus = paramsMap.get("trade_status");
        //获取notify_id
        String notifyId = paramsMap.get("notify_id");

        //根据out_trade_no , 支付类型 支付宝
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());
        if (signVerified) {
            //验签通过
            // TODO 验签成功后，按照支付结果异步通知中的描述，
            //  对支付结果中的业务内容进行二次校验，
            //  校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            if (paymentInfo == null || new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount)) != 0 ||
                    !app_id.equals(AlipayConfig.app_id)) {
                return "failure";
            }

            //存储notifyId到redis
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 1462, TimeUnit.MINUTES);
            //判断是否已经执行过了
            if(!flag){
                return "failure";
            }
            //判断一下状态
            if("TRADE_SUCCESS".equals(tradeStatus)||"TRADE_FINISHED".equals(tradeStatus)){
                //修改支付记录
                paymentService.paySuccess(out_trade_no, PaymentType.ALIPAY.name(),paramsMap);
                return "success";
            }
        } else {
            return "failure";
            // TODO 验签失败则记录异常日志，并在response中返回failure.
        }
        return "failure";
    }

    /**
     * 退款
     * @param orderId
     * @return
     */
    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId){
        boolean flag = alipayService.refund(orderId);
        return Result.ok(flag);
    }

    /**
     * 根据订单Id关闭订单
     * @param orderId
     * @return
     */
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId){
        Boolean aBoolean = alipayService.closePay(orderId);
        return aBoolean;
    }

    /**
     * 根据订单查询是否支付成功
     * @param orderId
     * @return
     */
    @GetMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId){
        // 调用退款接口
        boolean checkPayment = alipayService.checkPayment(orderId);
        return checkPayment;
    }

    /**
     * 整合关闭过期订单
     * @param outTradeNo
     * @return
     */
    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (null!=paymentInfo){
            return paymentInfo;
        }
        return null;
    }

}
