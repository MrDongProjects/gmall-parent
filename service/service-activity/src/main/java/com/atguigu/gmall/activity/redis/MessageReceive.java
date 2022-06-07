package com.atguigu.gmall.activity.redis;

import com.atguigu.gmall.activity.util.CacheHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class MessageReceive {

    /**
     * 接收消息的方法
     * @param message
     */
    public void receiveMessage(String message){
        System.out.println("message = " + message);
        if (!StringUtils.isEmpty(message)){
              /*
                消息格式
                skuId:0 表示没有商品
                skuId:1 表示有商品
             */
            // 因为传递过来的数据为 ""6:1""
            message = message.replaceAll("\"", "");
            String[] split = StringUtils.split(message, ":");
            if (split != null || split.length == 2){
                CacheHelper.put(split[0],split[1]);
            }
        }
    }

}
