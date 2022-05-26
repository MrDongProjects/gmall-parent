package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 用户登录管理
     * @param userInfo
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request, HttpServletResponse response){
        UserInfo user = userInfoService.login(userInfo);

        if (user != null){
            String token = UUID.randomUUID().toString().replace("-", "");
            HashMap<String, Object> map = new HashMap<>();

            map.put("nickName", user.getNickName());
            map.put("token", token);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", user.getId().toString());
            jsonObject.put("ip", IpUtil.getIpAddress(request));

            redisTemplate.opsForValue().set(RedisConst.USER_LOGIN_KEY_PREFIX + token,jsonObject.toJSONString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

            return Result.ok(map);
        }else {
            return Result.fail("用户名或密码错误!");
        }
    }

    @GetMapping("/logout")
    public Result logout(HttpServletRequest request){
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX + request.getHeader("token"));
        return Result.ok();
    }

}
