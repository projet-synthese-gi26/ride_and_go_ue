package com.yowyob.rideandgo.domain.ports.out;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface AuthPort {
    Mono<AuthResponse> login(String email, String password);
    
    Mono<Void> forgotPassword(String email);

    Mono<AuthResponse> register(
        String username, 
        String password, 
        String email, 
        String phone, 
        String firstName, 
        String lastName);

    
    record AuthResponse(
        String accessToken, 
        String refreshToken, 
        String username, 
        List<String> roles,
        List<String> permissions
    ) {}
}