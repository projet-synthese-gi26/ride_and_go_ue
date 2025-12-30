package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import java.util.List;

@Slf4j
public class FakeAuthAdapter implements AuthPort {
    @Override
    public Mono<AuthResponse> login(String email, String password) {
        log.info("🛠 MODE FAKE AUTH : Login pour {}", email);
        return Mono.just(new AuthResponse("fake-jwt", "fake-refresh", email, List.of("ADMIN"), List.of("*")));
    }

    @Override
    public Mono<AuthResponse> register(String username, String email, String password, String phone, String firstName, String lastName) {
        log.info("🛠 MODE FAKE AUTH : Inscription pour {}", username);
        return Mono.just(new AuthResponse(
            "fake-jwt", 
            "fake-refresh", 
            username, 
            List.of("ADMIN"), 
            List.of("*")));
    }

    @Override
    public Mono<Void> forgotPassword(String email) {
        return Mono.empty();
    }
}