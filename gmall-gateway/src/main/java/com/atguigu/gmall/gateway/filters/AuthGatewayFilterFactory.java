package com.atguigu.gmall.gateway.filters;

import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathesConfig> {

    @Autowired
    private JwtProperties properties;

    public AuthGatewayFilterFactory(){
        super(PathesConfig.class);
    }

    /**
     * 拦截的业务逻辑
     * @param config
     * @return
     */
    @Override
    public GatewayFilter apply(PathesConfig config) {
        return (ServerWebExchange exchange, GatewayFilterChain chain) -> {

            // 1.判断当前路径在不在拦截黑名单中，不在直接放行
            // HttpServletRequest == ServerHttpRequest  HttpServletResponse == ServerHttpResponse
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            // 获取当前路径
            String curPath = request.getURI().getPath();
            // 黑名单
            List<String> pathes = config.pathes;
            // 如果当前路径不在黑名单中，直接放行
            if (pathes.stream().allMatch(path -> curPath.indexOf(path) == -1)){
                return chain.filter(exchange);
            }

            // 2.获取cookie中token信息
            String token = request.getHeaders().getFirst("token");
            if (StringUtils.isBlank(token)){
                MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(this.properties.getCookieName())){
                    token = cookies.getFirst(this.properties.getCookieName()).getValue();
                }
            }

            // 3.判断token是否为空，拦截(重定向到登录页面)
            if (StringUtils.isBlank(token)){
                return interceptor(request, response);
            }

            try {
                // 4.解析jwt类型token获取用户信息
                Map<String, Object> map = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());

                // 5.判断当前用户ip和token中ip是否一致（防盗用）
                String ip = map.get("ip").toString();
                String curIp = IpUtil.getIpAddressAtGateway(request);
                if (!StringUtils.equals(ip, curIp)){
                    // 不一致说明被盗用，直接拦截
                    return interceptor(request, response);
                }

                // 6.把解析后的用户信息传递给后续服务
                String userId = map.get("userId").toString();
                String username = map.get("username").toString();
                // 把登录信息放入request头信息中传递给后续服务
                request = request.mutate().header("userId", userId).header("userName", username).build();
                exchange = exchange.mutate().request(request).build();
            } catch (Exception e) {
                e.printStackTrace();
                return interceptor(request, response);
            }
            // 7.放行
            return chain.filter(exchange);
        };
    }

    private Mono<Void> interceptor(ServerHttpRequest request, ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.SEE_OTHER);
        response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin?returnUrl=" + request.getURI());
        // 请求结束返回Mono<void>
        return response.setComplete();
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("pathes");
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Data
    @ToString
    public static class PathesConfig{
        private List<String> pathes;
    }
}
