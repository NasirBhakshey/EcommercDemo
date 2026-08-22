package com.ecommerce.apigateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil){
        this.jwtUtil = jwtUtil;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // Public endpoints do not require JWT
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        // No Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            return chain.filter(exchange);
        }

        String email = jwtUtil.extractEmail(token);

        List<String> roles = jwtUtil.extractRoles(token);

        if (roles == null) {
            roles = Collections.emptyList();
        }

        List<SimpleGrantedAuthority> authorities =
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority(
                                "ROLE_" + role
                        ))
                        .toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authorities
                );

        return chain.filter(exchange)
                .contextWrite(
                        ReactiveSecurityContextHolder
                                .withAuthentication(authentication)
                );
    }

    private boolean isPublicPath(String path) {
        return path.equals("/auth/login")
                || path.equals("/auth/register")
                || path.equals("/actuator/health");
    }
}
