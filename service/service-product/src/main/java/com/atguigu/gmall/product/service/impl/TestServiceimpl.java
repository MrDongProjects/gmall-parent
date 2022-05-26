package com.atguigu.gmall.product.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.atguigu.gmall.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TestServiceimpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void testLock() {
        //查询redis中的num值
        String num = redisTemplate.opsForValue().get("num");
        //如果num值为空return
        if (StringUtils.isBlank(num)) {
            return;
        }
        //有值转成int类型
        int numInt = Integer.parseInt(num);
        //把redis中的num值+1
        redisTemplate.opsForValue().set("num", String.valueOf(++numInt));
    }
}
