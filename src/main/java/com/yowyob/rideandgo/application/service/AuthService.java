package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.AuthUseCase;
import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import com.yowyob.rideandgo.domain.ports.out.DriverRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.PaymentPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final AuthPort authPort;
    private final PaymentPort paymentPort;
    private final DriverRepositoryPort driverRepositoryPort;

    @Override
    public Mono<AuthPort.AuthResponse> login(String identifier, String password) {
        return authPort.login(identifier, password);
    }

    @Override
    public Mono<AuthPort.AuthResponse> refreshToken(String refreshToken) {
        return authPort.refreshToken(refreshToken);
    }

    @Override
    public Mono<AuthPort.AuthResponse> register(String username, String email, String password, String phone,
            String firstName, String lastName, List<RoleType> roles, FilePart photo) {

        return authPort.register(username, password, email, phone, firstName, lastName, roles, photo)
                .flatMap(response -> {
                    boolean isDriver = roles.contains(RoleType.RIDE_AND_GO_DRIVER);

                    if (isDriver) {
                        log.info("🚕 Driver registered, initializing profile for {}", response.userId());

                        // 1. On crée explicitement l'entrée dans la table 'drivers'
                        return driverRepositoryPort.createDriver(response.userId())
                                // 2. On crée le wallet
                                .flatMap(d -> {
                                    log.info("💳 Creating wallet for driver {}", response.userId());
                                    return paymentPort.createWallet(response.userId(), response.username());
                                })
                                .thenReturn(response)
                                .onErrorResume(e -> {
                                    log.error("⚠️ Driver initialization warning: {}", e.getMessage());
                                    // On ne bloque pas l'inscription si le wallet échoue (ex: déjà existant)
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