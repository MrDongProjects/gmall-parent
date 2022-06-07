package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConfirmReceiver {


    /**
     * 监听消息
     * @param message
     * @param channel
     *
     *  @RabbitListener： 消息监听
     *  @Queue：初始化消息队列
     *  @Exchange：初始化交换机
     *  @QueueBinding：将队列根据路由绑定到交换机
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm",autoDelete = "false"),
            key = {"routing.confirm"}
    ))
    @SneakyThrows
    public void proccess(Message message, Channel channel){

        System.out.println("ConfirmReceiver:接收的数据==="+new String(message.getBody()));

        //手动确认
        /**
         * message.getMessageProperties().getDeliveryTag()：消息序列号
         * 是否批量回复
         */
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}
