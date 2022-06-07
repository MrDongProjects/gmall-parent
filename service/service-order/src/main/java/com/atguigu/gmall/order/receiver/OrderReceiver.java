package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@SuppressWarnings("all")
public class OrderReceiver {
    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;
    //监听消息
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void cancelOrder(Long orderId, Message message, Channel channel){
        try {
            //判断
            if(orderId!=null){
                //获取订单
                OrderInfo orderInfo = orderService.getById(orderId);
                //判断订单
                if(orderInfo!=null&&"UNPAID".equals(orderInfo.getOrderStatus())
                        &&"UNPAID".equals(orderInfo.getProcessStatus())){

                    //查询支付记录信息
                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    //判断
                    if(null!=paymentInfo && "UNPAID".equals(paymentInfo.getPaymentStatus())){
                        //查询支付宝交易记录
                        Boolean aBoolean = paymentFeignClient.checkPayment(orderId);
                        //判断
                        if(aBoolean){
                            //支付已经交易记录但是未支付
                            //关闭支付宝交易记录
                            Boolean aBoolean1 = paymentFeignClient.closePay(orderId);
                            //关闭
                            if(aBoolean1){
                                orderService.execExpiredOrder(orderId,"2");
                            }else{
                                //用户已经付款，发送短信
                                //修改订单状态
                            }
                        }else{
                            //关闭交易记录
                            orderService.execExpiredOrder(orderId,"2");
                        }
                    }else{
                        //修改订单状态 关闭订单
                        orderService.execExpiredOrder(orderId,"1");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //日志收集异常，通知开发处理
        }

        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //订单支付成功,更改订单状态
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updateOrder(Long orderId,Message message,Channel channel){
        //判断orderId
        if (orderId != null){
            //更新订单状态+进度
            OrderInfo orderInfo = orderService.getById(orderId);
            //判断状态
            if (orderInfo!=null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                //更新状态为支付
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                orderService.sendOrderStatu(orderId);
            }
        }
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //发送减库存通知
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrderStatus(String msgJson,Message message,Channel channel){
        if (!StringUtils.isEmpty(msgJson)){
            Map<String, Object> map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");
//            String parentOrderId = (String) map.get("parentOrderId");
            OrderInfo orderInfo = new OrderInfo();
            if ("DEDUCTED".equals(status)){
                // 减库存成功！ 修改订单状态为已支付
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
//                orderInfo.setUserId(orderService.getNowUserId(Long.parseLong(orderId)));
//                orderService.setUserIdWithParent(orderInfo);
            }else {
                //减库存失败！远程调用其他仓库查看是否有库存！
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.STOCK_EXCEPTION);
            }

        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
