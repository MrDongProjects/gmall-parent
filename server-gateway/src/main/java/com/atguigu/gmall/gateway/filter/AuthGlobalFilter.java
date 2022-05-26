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

    @Autowired
    private RedisTemplate redisTemplate;

    //匹配路径工具
    AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Value("${authUrls.url}")
    private String authUrls;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        //获取url
        String path = request.getURI().getPath();
        //如果是内部接口,则网关拦截不允许外部访问
        if (antPathMatcher.match("/**/inner/**", path)){
            ServerHttpResponse response = exchange.getResponse();
            return out(response, ResultCodeEnum.PERMISSION);
        }
        //获取用户Id
        String userId = getUserId(request);
        //token被盗用
        if ("-1".equals(userId)){
            ServerHttpResponse response = exchange.getResponse();
            return out(response,ResultCodeEnum.PERMISSION);
        }
        //用户登陆认证
        //api接口,异步请求,校验用户必须登陆
        if (antPathMatcher.match("/api/**/auth/**", path)){
            if (StringUtils.isEmpty(userId)){
                ServerHttpResponse response = exchange.getResponse();
                return out(response,ResultCodeEnum.LOGIN_AUTH);
            }
        }

        //验证url白名单
        for (String authUrl : authUrls.split(",")) {
            //当前的url包含登陆的控制器域名,但是用户id为空
            if (path.indexOf(authUrl)!=-1 && StringUtils.isEmpty(userId)){
                ServerHttpResponse response = exchange.getResponse();
                //303状态吗表示由于请求对应的资源存在另一个URI,应使用重定向获取请求的资源
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION, "http://www.gmall.com/login.html?originUrl=" + request.getURI());
                //重定向到登陆
                return response.setComplete();
            }
        }
        //将userid传递给后端
        if (!StringUtils.isEmpty(userId)){
            request.mutate().header("userId", userId).build();
            //将现在的request变成exchange对象
            return chain.filter(exchange.mutate().request(request).build());
        }
        return chain.filter(exchange);
    }

    /**
     * 获取当前登录用户id
     * @param request
     * @return token有可能从头信息中传递携带
     *      有可能从cookie中携带
     *      1.已经登录 userId
     *      2.未登录 “”
     *      3.被盗用 -1
     */
    private String getUserId(ServerHttpRequest request) {
        String token = null;
        List<String> tokenList = request.getHeaders().get("userId");
        if (!CollectionUtils.isEmpty(tokenList)){
            token = tokenList.get(0);
        }else {
            //从cookies中获取
            //获取所有的cookies
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            //获取指定的cookie
            HttpCookie tokenCookies = cookies.getFirst("token");
            if (tokenCookies != null){
                token = URLDecoder.decode(tokenCookies.getValue());


            }

        }
        //根据token从redis中获取数据
        if (!StringUtils.isEmpty(token)){
            String tokenStr = (String) redisTemplate.opsForValue().get("user:login:" +token);
            //转换数据类型
            JSONObject jsonObject = JSONObject.parseObject(tokenStr);
            //校验ip
            String ip = jsonObject.getString("ip");
            //获取本次请求的ip
            String gatwayIpAddress = IpUtil.getGatwayIpAddress(request);
            //判断是否盗用了token
            if (!ip.equals(gatwayIpAddress)){
                return "-1";
            }else {
                return jsonObject.getString("userId");
            }
        }
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
