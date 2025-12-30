package com.yowyob.rideandgo.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    // Simulation de tokens liés à des UUIDs réels de ta base (issus du seeding data.sql)
    private static final Map<String, String> STATIC_TOKENS = Map.of(
        "client-token", "7f13909e-7170-4f91-872e-333333333333", // Un UUID de customer
        "driver-1-token", "a1b2c3d4-e5f6-4a5b-8c9d-111111111111", // Un UUID de driver
        "driver-2-token", "a1b2c3d4-e5f6-4a5b-8c9d-222222222222"  // Un autre UUID de driver
    );

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        
        // Si le token est connu, on retourne l'UUID correspondant comme "Name"
        if (STATIC_TOKENS.containsKey(authToken)) {
            String userId = STATIC_TOKENS.get(authToken);
            return Mono.just(new UsernamePasswordAuthenticationToken(
                    userId, 
                    null, 
                    List.of(new SimpleGrantedAuthority("ROLE"))
            ));
        }

        // Fallback pour ne pas bloquer les autres tests
        if (authToken != null && !authToken.isEmpty()) {
            return Mono.just(new UsernamePasswordAuthenticationToken(
                    authToken, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
            ));
        }
        
        return Mono.empty();
    }
}