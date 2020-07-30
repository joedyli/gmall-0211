package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsWebConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){
        // cors配置类
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 允许跨域访问的域名。不要写*，写*无法携带cookie
        corsConfiguration.addAllowedOrigin("http://manager.gmall.com");
        corsConfiguration.addAllowedOrigin("http://gmall.com");
        corsConfiguration.addAllowedOrigin("http://www.gmall.com");
        // 是否允许携带cookie
        corsConfiguration.setAllowCredentials(true);
        // 允许访问的方式
        corsConfiguration.addAllowedMethod("*");
        // 允许携带的头信息
        corsConfiguration.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsWebFilter(configurationSource);
    }
}
