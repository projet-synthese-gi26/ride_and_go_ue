package com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

@HttpExchange("/api")
public interface AuthApiClient {
    
    @PostExchange("/auth/login")
    Mono<TraMaSysResponse> login(@RequestBody LoginRequest request);

    @PostExchange("/auth/register")
    Mono<TraMaSysResponse> register(@RequestBody RegisterRequest request);

    @GetExchange("/users/service/{serviceName}")
    Flux<UserDetail> getUsersByService(@PathVariable String serviceName);

    @GetExchange("/users/{id}")
    Mono<UserDetail> getUserById(@PathVariable String id);

    // --- DTOs Internes ---

    record LoginRequest(String identifier, String password) {}

    record RegisterRequest(
        String username,
        String password,
        String email,
        String phone,
        String firstName,
        String lastName,
        String service, // Fixé à "RIDE_AND_GO"
        List<String> roles
    ) {}

    // Structure de réponse standard du service Auth
    record TraMaSysResponse(
        String accessToken, 
        String refreshToken, 
        UserDetail user
    ) {}

    record UserDetail(
        String id, 
        String username, 
        String email, 
        String phone,
        String firstName,
        String lastName,
        String service,
        List<String> roles, 
        List<String> permissions
    ) {}
}