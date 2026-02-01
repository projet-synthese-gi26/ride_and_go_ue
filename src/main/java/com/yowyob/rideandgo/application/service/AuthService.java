package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.AuthUseCase;
import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import com.yowyob.rideandgo.domain.ports.out.PaymentPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final AuthPort authPort;
    private final PaymentPort paymentPort;

    @Override
    public Mono<AuthPort.AuthResponse> login(String identifier, String password) {
        return authPort.login(identifier, password);
    }

    @Override
    public Mono<AuthPort.AuthResponse> refreshToken(String refreshToken) {
        return authPort.refreshToken(refreshToken);
    }

    public Mono<AuthPort.AuthResponse> register(String username, String email, String password, String phone,
            String firstName, String lastName, List<RoleType> roles) {
        return authPort.register(username, password, email, phone, firstName, lastName, roles)
                .flatMap(response -> {
                    // CAS 1 : Création Wallet lors du Register si le rôle DRIVER est demandé
                    boolean isDriver = roles.contains(RoleType.RIDE_AND_GO_DRIVER);

                    if (isDriver) {
                        log.info("💳 Registering a Driver: Triggering Wallet creation for {}", response.userId());
                        return paymentPort.createWallet(response.userId(), response.username())
                                .thenReturn(response)
                                .onErrorResume(e -> {
                                    log.error("⚠️ Wallet creation failed during register, but continuing: {}",
                                            e.getMessage());
                                    return Mono.just(response);
                                });
                    }
                    return Mono.just(response);
                });
    }

    @Override
    public Mono<Void> resetPassword(String email) {
        return authPort.forgotPassword(email);
    }
}