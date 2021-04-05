package com.example.gulimall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.Map;

/**
 * 解决前后端分离跨域问题
 */
@Configuration
public class CorsConfiguretionDAY {
    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        //配置跨域
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("*");//允许那些请求头跨域
        config.addAllowedMethod("*");//允许那些请求方式跨域
        config.addAllowedOrigin("*");//允许那些请求来源跨域
        config.setAllowCredentials(true);//是否允许携带cookie进行跨域
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",config);
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }
}
