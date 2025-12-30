package com.yowyob.rideandgo.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Repository responsible for extracting the JWT token from the Authorization header
 * and initiating the authentication process.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private final JwtAuthenticationManager authenticationManager;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.error(new UnsupportedOperationException("Stateless API: session saving is not supported."));
    }

    /**
     * Loads the security context by extracting the "Bearer" token from HTTP headers.
     */
    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .flatMap(authHeader -> {
                    String authToken = authHeader.substring(7);
                    // Create an unauthenticated token object
                    Authentication auth = new UsernamePasswordAuthenticationToken(authToken, authToken);
                    // Delegate validation to the AuthenticationManager
                    return this.authenticationManager.authenticate(auth)
                            .map(SecurityContextImpl::new);
                });
    }
}