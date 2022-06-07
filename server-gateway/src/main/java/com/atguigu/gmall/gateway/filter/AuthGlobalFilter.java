package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter {

    @Value("${authUrls.url}")
    private String authUrls;

    //匹配路径工具
    AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 实现资源的过滤，用户的认证
     *
     * @param exchange
     * @param chain
     * @return http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
     * http://api.gmall.com/api/user/passport/login
     * getPath()::api/user/passport/login
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        //获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        //获取请求的资源路径
        String path = request.getURI().getPath();//login.html

        //如果是内部接口，拦截不允许访问 /**/inner/**
        if (antPathMatcher.match("/**/inner/**", path)) {
            ServerHttpResponse response = exchange.getResponse();

            return out(response, ResultCodeEnum.PERMISSION);
        }

        //校验用户是否登录
        String userId = getUserId(request);
        //判断结果是否被盗用
        if ("-1".equals(userId)) {

            ServerHttpResponse response = exchange.getResponse();
            return out(response, ResultCodeEnum.PERMISSION);
        }
        //api认证接口
        if (antPathMatcher.match("/api/**/auth/**", path)) {
            //判断是否的登录
            if (StringUtils.isEmpty(userId)) {

                ServerHttpResponse response = exchange.getResponse();
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }

        }
        //验证白名单 authUrls  trade.html,myOrder.html ,list.html
        for (String authUrl : authUrls.split(",")) {

            //第一个当前请求资源路径包含白名单中的资源
            //第二个 用户未登录
            if(path.indexOf(authUrl)!=-1&&StringUtils.isEmpty(userId)){

                ServerHttpResponse response = exchange.getResponse();
                //设置重定向 状态码
                response.setStatusCode(HttpStatus.SEE_OTHER);
                //重定向的路径
                response.getHeaders().set(HttpHeaders.LOCATION,
                        "http://www.gmall.com/login.html?originUrl="+request.getURI());

                //重定向
                return  response.setComplete();
            }


        }
        //获取临时userTempId
        String userTempId=getUserTempId(request);



        //设置携带userId 和userTempId
        if(!StringUtils.isEmpty(userId)||!StringUtils.isEmpty(userTempId)){


            if(!StringUtils.isEmpty(userTempId)){
                request.mutate().header("userTempId",userTempId);
            }
            if(!StringUtils.isEmpty(userId)){

                request.mutate().header("userId",userId);
            }


            //放行
            return chain.filter(exchange.mutate().request(request).build());
        }

        //最终放行
        return chain.filter(exchange);
    }

    /**
     * 获取临时id
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {
        //定义变量接收userTemId
        String userTempId="";
        //从头信息中获取
        List<String> userTempIdList = request.getHeaders().get("userTempId");
        //判断
        if(!CollectionUtils.isEmpty(userTempIdList)){
            userTempId =userTempIdList.get(0);
        }else{
            //尝试从cookie中获取
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            //获取userTempId
            HttpCookie cookie = cookies.getFirst("userTempId");
            //判断
            if(cookie!=null){

                userTempId= URLDecoder.decode(cookie.getValue());
            }

        }


        return userTempId;
    }

    /**
     * 根据token获取用户userId
     *
     * @param request
     * @return token有可能从头信息中传递携带
     * 有可能从cookie中携带
     * 1.已经登录 userId
     * 2.未登录 “”
     * 3.被盗用 -1
     */
    private String getUserId(ServerHttpRequest request) {

        //定义变量，接收token
        String token = null;
        //获取token
        List<String> tokenList = request.getHeaders().get("token");
        //判断
        if (!CollectionUtils.isEmpty(tokenList)) {

            token = tokenList.get(0);
        } else {
            //从cookie中获取
            //获取所有的cookie
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            //获取指定的cookie
            HttpCookie tokenCookie = cookies.getFirst("token");
            //判断
            if (tokenCookie != null) {
                //885328e18890417482542f854e80ea98  //asdfsdafdsafewtredare543543256436^*(&^*7
                token = URLDecoder.decode(tokenCookie.getValue());
            }

        }


        //根据token从redis中获取数据

        if (!StringUtils.isEmpty(token)) {

            String tokenStr = (String) redisTemplate.opsForValue().get("user:login:" + token);
            //转换类型
            JSONObject jsonObject = JSONObject.parseObject(tokenStr);
            //校验ip
            String ip = jsonObject.getString("ip");
            //获取本次请求的ip
            String curIp = IpUtil.getGatwayIpAddress(request);

            //判断是否盗用了token
            if (!ip.equals(curIp)) {

                return "-1";
            } else {

                return jsonObject.getString("userId");
            }


        }


        //token没有获取，直接返回空字符串
        return "";
    }

    /**
     * 设置输出响应 数据
     *
     * @param response
     * @param permission
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum permission) {

        //构建响应数据
        Result result = Result.build(null, permission);
        //设置数据编码
        byte[] bytes = JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        //数据转换
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        //中文处理
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        return response.writeWith(Mono.just(buffer));
    }

}
