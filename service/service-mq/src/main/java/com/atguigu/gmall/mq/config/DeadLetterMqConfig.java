package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DeadLetterMqConfig {

    // 声明一些变量

    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";
    //定义交换机
    @Bean
    public DirectExchange exchange(){

        return new DirectExchange("exchange_dead",true,false,null);
    }

    //定义队列
    @Bean
    public Queue queue1(){
        //设置参数
        Map<String ,Object> map=new HashMap<>();
        //设置死信交换机
        map.put("x-dead-letter-exchange",exchange_dead);
        //设置死信路由
        map.put("x-dead-letter-routing-key",routing_dead_2);
        //设置消息超时
        map.put("x-message-ttl",10*1000);


        return new Queue(queue_dead_1,true,false,false,map);
    }

    //创建队列2
    @Bean
    public Queue queue2(){

        return new Queue(queue_dead_2,true,false,false,null);
    }


    //绑定queue1
    @Bean
    public Binding binding(){

        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }

    //绑定queue2
    @Bean
    public Binding binding2(){

        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }



}
