package com.devpro.api_gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class AuthMiddleware  implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return  exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(auth ->
                        {
                            Jwt jwt = auth.getToken();

                            ServerHttpRequest request = exchange.getRequest().mutate()
                                    .header("X-User-Id", jwt.getSubject()) //id
                                    .header("X-User-email", jwt.getClaimAsString("email")) //email
                                    .build();

                            return  exchange.mutate().request(request).build();
                        })
                .flatMap(chain::filter);
    }
}
