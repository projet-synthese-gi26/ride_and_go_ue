package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.AuthApiClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import java.util.List;

import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
public class RemoteAuthAdapter implements AuthPort {

    private final AuthApiClient authApiClient;

    public RemoteAuthAdapter(AuthApiClient authApiClient) {
        this.authApiClient = authApiClient;
    }

    @Override
    public Mono<AuthResponse> login(String identifier, String password) {
        return authApiClient.authenticate(new AuthApiClient.LoginRequest(identifier, password))
                .map(this::mapToDomain);
    }

    @Override
     public Mono<AuthResponse> register(String username, String email, String password, String phone, String firstName, String lastName) {
        // CORRECTION DE L'ORDRE : username, password, email (et non email, password)
        AuthApiClient.RegisterRequest request = new AuthApiClient.RegisterRequest(
            username, 
            password, 
            email,    
            phone, 
            firstName, 
            lastName,
            "FLEET_MANAGEMENT", 
            List.of("ADMIN") 
        );

       return authApiClient.register(request)
            .map(this::mapToDomain)
            .doOnSuccess(res -> log.info("✅ Inscription réussie : {}", res.username()))
            .onErrorResume(WebClientResponseException.class, ex -> {
                // On logue le message d'erreur détaillé venant du serveur distant
                String errorBody = ex.getResponseBodyAsString();
                log.error("❌ Erreur TraMaSys (400) : {}",ex.getStatusCode(), errorBody);
                return Mono.error(new RuntimeException("Erreur TraMaSys : " + errorBody));
            });
    }

    private AuthResponse mapToDomain(AuthApiClient.TraMaSysResponse res) {
        return new AuthResponse(
            res.accessToken(),
            res.refreshToken(),
            res.user().username(),
            res.user().roles(),
            res.user().permissions()
        );
    }

    @Override
    public Mono<Void> forgotPassword(String email) {
        log.info("Appel forgot password pour {}", email);
        return Mono.empty();
    }
}