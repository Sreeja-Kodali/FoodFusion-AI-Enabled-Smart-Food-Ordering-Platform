package com.foodfusion.apigateway.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserIdentityForwardingFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitized = exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Roles");
                }))
                .build();
        return sanitized.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> withIdentityHeaders(sanitized, authentication))
                .defaultIfEmpty(sanitized)
                .flatMap(chain::filter);
    }

    private ServerWebExchange withIdentityHeaders(
            ServerWebExchange exchange,
            JwtAuthenticationToken authentication
    ) {
        String roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.joining(","));
        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.set("X-User-Id", authentication.getToken().getSubject());
                    headers.set("X-User-Roles", roles);
                }))
                .build();
    }
}
