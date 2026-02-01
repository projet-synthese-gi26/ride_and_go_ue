package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.AuthUseCase;
import com.yowyob.rideandgo.domain.ports.out.AuthPort;
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

    @Override
    public Mono<AuthPort.AuthResponse> login(String identifier, String password) {
        return authPort.login(identifier, password);
    }

    @Override
    public Mono<AuthPort.AuthResponse> refreshToken(String refreshToken) {
        return authPort.refreshToken(refreshToken);
    }

    @Override // ✅ Signature mise à jour
    public Mono<AuthPort.AuthResponse> register(String username, String email, String password, String phone,
            String firstName, String lastName, List<RoleType> roles, FilePart photo) {
        
        return authPort.register(username, password, email, phone, firstName, lastName, roles, photo)
                .flatMap(response -> {
                    boolean isDriver = roles.contains(RoleType.RIDE_AND_GO_DRIVER);

                    if (isDriver) {
                        log.info("💳 Driver registered, creating wallet for {}", response.userId());
                        return paymentPort.createWallet(response.userId(), response.username())
                                .thenReturn(response)
                                .onErrorResume(e -> {
                                    log.error("⚠️ Wallet creation failed, continuing: {}", e.getMessage());
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