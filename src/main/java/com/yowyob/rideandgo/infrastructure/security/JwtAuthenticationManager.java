package com.yowyob.rideandgo.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        
        // TODO: validate token
        if (authToken != null && !authToken.isEmpty()) {
            return Mono.just(new UsernamePasswordAuthenticationToken(
                    "fake_user_rideandgo", 
                    null, 
                    List.of(new SimpleGrantedAuthority("ADMIN")) 
            ));
        }
        return Mono.empty();
    }
}