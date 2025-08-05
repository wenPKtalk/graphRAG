package com.topsion.rag.security.jwt;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JWTReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final TokenProvider tokenProvider;

    public JWTReactiveAuthenticationManager(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.justOrEmpty(authentication)
            .cast(BearerTokenAuthenticationToken.class)
            .flatMap(this::authenticateToken);
    }

    private Mono<Authentication> authenticateToken(BearerTokenAuthenticationToken token) {
        String jwt = token.getToken();
        return Mono.just(jwt)
            .filter(tokenProvider::validateToken)
            .map(tokenProvider::getAuthentication);
    }
}