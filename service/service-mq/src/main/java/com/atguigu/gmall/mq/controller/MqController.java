package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 基于延迟插件发送延迟消息
     *
     * @return
     */
    @GetMapping("sendDelay")
    public Result sendDelay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay, sdf.format(new Date()), new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setDelay(10 * 1000);
                System.out.println(sdf.format(new Date()) + " Delay sent.");
                return message;
            }
        });
        return Result.ok();
    }


    /**
     * 基于死信实现延迟消息
     *
     * @return
     */
    @GetMapping("/deadLetter")
    public Result sendDeadLetter() {

        //格式化时间对象
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        this.rabbitTemplate.convertAndSend(
                DeadLetterMqConfig.exchange_dead,
                DeadLetterMqConfig.routing_dead_1, "ok");

        System.out.println("发送时间：" + format.format(new Date()));
        return Result.ok();
    }

    /**
     * 发送消息
     *
     * @return
     */
    @GetMapping("/sendConfirm")
    public Result sendConfirm() {

        rabbitService.sendMessage("exchange.confirm", "routing.confirm", "确认消息发送的内容");

        return Result.ok();

    }
}
