package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.sound.midi.Soundbank;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Configuration
public class DeadLetterReceiver {

    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    @SneakyThrows
    public void get(String msg, Message message, Channel channel){
        System.out.println("接收到的消息："+msg);

        //格式化时间对象
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println("接收消息的时间："+format.format(new Date()));


        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }

}
