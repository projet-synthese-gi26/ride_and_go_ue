package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.ports.in.AuthUseCase;
import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    public Mono<AuthPort.AuthResponse> login(@RequestBody LoginRequest request) {
        return authUseCase.login(request.identifier(), request.password());
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription Fleet Management")
    public Mono<AuthPort.AuthResponse> register(@RequestBody RegisterDto dto) {
        return authUseCase.register(
            dto.username(), dto.password(), dto.email(), 
            dto.phone(), dto.firstName(), dto.lastName()
        );
    }

    public record LoginRequest(String identifier, String password) {}
    
    public record RegisterDto(
        String username, 
        String password, 
        String email, 
        String phone, 
        String firstName, 
        String lastName
    ) {}
}