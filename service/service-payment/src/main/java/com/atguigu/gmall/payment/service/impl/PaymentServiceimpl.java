package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.constant.MqConst;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
@SuppressWarnings("all")
public class PaymentServiceimpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    /**
     * 保存交易记录
     * @param orderInfo
     * @param paymentType 支付类型（1：微信 2：支付宝）
     */
    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderInfo.getId());
        queryWrapper.eq("payment_type", paymentType);
        //根据订单ID和支付类型查询当前是否已经存在
        Integer count = paymentInfoMapper.selectCount(queryWrapper);
        //如果存在 则 不保存
        if (count > 0) return;

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());

        paymentInfoMapper.insert(paymentInfo);

    }

    /**
     * 获取paymentInfo 对象
     * @param tradeNo
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(String tradeNo, String name) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("out_trade_no", tradeNo);
        queryWrapper.eq("payment_type", name);
        return paymentInfoMapper.selectOne(queryWrapper);
    }

    /**
     * 付成功更新交易记录方法
     * @param tradeNo
     * @param name
     * @param paramMap
     */
    @Override
    public void paySuccess(String tradeNo, String name, Map<String, String> paramMap) {
        //  根据outTradeNo，paymentType 查询
        PaymentInfo paymentInfoQuery = getPaymentInfo(tradeNo, name);
        if (paymentInfoQuery == null){
            return;
        }
        try {
            //  改造一下更新的方法！
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
            paymentInfo.setCallbackContent(paramMap.toString());
            paymentInfo.setTradeNo(paramMap.get("trade_no"));
            //  save
            updatePaymentInfo(tradeNo,name,paymentInfo);
        } catch (Exception e) {
            //  删除key
            redisTemplate.delete(paramMap.get("notify_id"));
            e.printStackTrace();
        }
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());

    }

    /**
     * 更新订单详情
     * @param tradeNo
     * @param name
     * @param paymentInfo
     */
    @Override
    public void updatePaymentInfo(String tradeNo, String name, PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("out_trade_no", tradeNo);
        queryWrapper.eq("payment_type", name);
        paymentInfoMapper.update(paymentInfo,queryWrapper);

    }

    /**
     * 关闭订单
     * @param orderId
     */
    @Override
    public void closePayment(Long orderId) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper();
        queryWrapper.eq("order_id", orderId);

        // 如果当前的交易记录不存在，则不更新交易记录
        Integer count = paymentInfoMapper.selectCount(queryWrapper);
        if (count == null || count.intValue() == 0) return;
        // 在关闭支付宝交易之前。还需要关闭paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
        paymentInfoMapper.update(paymentInfo, queryWrapper);
    }
}
