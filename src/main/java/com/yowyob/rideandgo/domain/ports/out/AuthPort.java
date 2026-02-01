package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.UUID;

public interface AuthPort {
    Mono<AuthResponse> login(String email, String password);

    Mono<Void> forgotPassword(String email);

    Mono<AuthResponse> refreshToken(String refreshToken);

    Mono<AuthResponse> register(
            String username,
            String password,
            String email,
            String phone,
            String firstName,
            String lastName,
            List<RoleType> roles);

    record AuthResponse(
            UUID userId, // Ajouté pour identifier l'utilisateur
            String accessToken,
            String refreshToken,
            String username,
            List<String> roles,
            List<String> permissions) {
    }
}