package com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client;

import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;
import java.util.List;

@HttpExchange("/api/auth")
public interface AuthApiClient {
    
    @PostExchange("/login")
    Mono<TraMaSysResponse> authenticate(@RequestBody LoginRequest request);

    @PostExchange("/register")
    Mono<TraMaSysResponse> register(@RequestBody RegisterRequest request);

    record LoginRequest(String identifier, String password) {}

    record RegisterRequest(
        String username,
        String password,
        String email,
        String phone,
        String firstName,
        String lastName,
        String service, // Sera "FLEET_MANAGEMENT"
        List<String> roles
    ) {}

    record TraMaSysResponse(String accessToken, String refreshToken, UserDetail user) {}
    record UserDetail(String id, String username, List<String> roles, List<String> permissions) {}
}