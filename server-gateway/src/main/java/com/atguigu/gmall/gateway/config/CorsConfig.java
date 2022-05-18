package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * @Author Echo
 * @Create 2022-05-14-13:54
 * @Description
 */
@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter(){
        //CORS跨域配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");//设置允许访问的网络
        corsConfiguration.setAllowCredentials(true);//设置是否从服务器获取
        corsConfiguration.addAllowedMethod("*");//设置请求方法 *表示任意
        corsConfiguration.addAllowedHeader("*");//所有请求头信息

        //配置源对象
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        //cors过滤对象
        return new CorsWebFilter(source);

    }
}
