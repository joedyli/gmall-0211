package com.atguigu.gmall.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(1)
public class MyGlobalFilter implements GlobalFilter {

    /**
     * 过滤器的业务逻辑
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        System.out.println("这是全局过滤器，所有经过网关的请求都会被我拦截。。。");

        return chain.filter(exchange);
    }

    /**
     * 返回值越小，过滤器的优先级越高
     * @return
     */
//    @Override
//    public int getOrder() {
//        return 1;
//    }
}
