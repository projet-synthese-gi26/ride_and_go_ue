package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.UUID;

@Slf4j
public class FakeAuthAdapter implements AuthPort {
    @Override
    public Mono<AuthResponse> login(String email, String password) {
        log.info("🛠 MODE FAKE AUTH : Login pour {}", email);
        return Mono.just(new AuthResponse(
                "fake.jwt.token",
                "fake.refresh.token",
                email,
                List.of("RIDE_AND_GO_DRIVER"),
                List.of("*")));
    }

    @Override
    public Mono<AuthResponse> register(String username, String email, String password, String phone, String firstName,
            String lastName, List<RoleType> roles) {
        log.info("🛠 MODE FAKE AUTH : Inscription pour {} avec rôles {}", username, roles);

        List<String> rolesStr = roles.stream().map(Enum::name).toList();

        return Mono.just(new AuthResponse(
                "fake.jwt.token",
                "fake.refresh.token",
                username,
                rolesStr,
                List.of("*")));
    }

    @Override
    public Mono<AuthResponse> refreshToken(String refreshToken) {
        log.info("🛠 MODE FAKE AUTH : Refresh Token");
        return Mono.just(new AuthResponse(
                "new.fake.jwt.token." + UUID.randomUUID(),
                "new.fake.refresh.token." + UUID.randomUUID(),
                "RefreshUser",
                List.of("RIDE_AND_GO_DRIVER"),
                List.of("*")));
    }

    @Override
    public Mono<Void> forgotPassword(String email) {
        return Mono.empty();
    }
}