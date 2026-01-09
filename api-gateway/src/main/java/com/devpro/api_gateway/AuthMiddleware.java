package com.devpro.api_gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthMiddleware implements GlobalFilter, Ordered {

    @Value("${admin.key}")
    private String adminApiKey;

    @Value("${admin.service.key}")
    private String adminServiceKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        System.out.println(path + " : path");

        if(path.startsWith("/api/problems")){
            String adminKey = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-API-KEY");

            System.out.println("key : " + adminKey);

            if(adminKey != null){
                System.out.println("key not null");
                System.out.println(adminApiKey.equals(adminKey) + " : " + adminApiKey);
                if (!adminApiKey.equals(adminKey)) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                ServerHttpRequest request = exchange.getRequest()
                        .mutate()
                        .header("X-Admin-service-key", adminServiceKey)
                        .header("X-Admin", "true")
                        .build();

                return chain.filter(exchange.mutate().request(request).build());
            }


        }

        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(auth -> {
                    Jwt jwt = auth.getToken();

                    ServerHttpRequest request = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id", jwt.getSubject())
                            .build();

                    return exchange.mutate().request(request).build();
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
