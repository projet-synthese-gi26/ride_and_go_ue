package com.yowyob.rideandgo.domain.ports.in;

import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import reactor.core.publisher.Mono;

public interface AuthUseCase {
    Mono<AuthPort.AuthResponse> login(String identifier, String password);
    
    Mono<Void> resetPassword(String email);

    // CETTE MÉTHODE DOIT EXISTER ICI AUSSI
    Mono<AuthPort.AuthResponse> register(
        String username, 
        String password, 
        String email, 
        String phone, 
        String firstName, 
        String lastName
    );
}