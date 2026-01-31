package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.AuthUseCase;
import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

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
    @Operation(summary = "Inscription Multi-Rôles")
    public Mono<AuthPort.AuthResponse> register(@RequestBody RegisterDto dto) {
        // Gestion par défaut : Si aucune liste n'est fournie, on assigne PASSENGER
        List<RoleType> rolesToAssign = (dto.roles() != null && !dto.roles().isEmpty())
                ? dto.roles()
                : List.of(RoleType.RIDE_AND_GO_PASSENGER); // dto.roles();

        return authUseCase.register(
                dto.username(), dto.password(), dto.email(),
                dto.phone(), dto.firstName(), dto.lastName(),
                rolesToAssign);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Uses a valid Refresh Token to generate a new Access Token.")
    public Mono<AuthPort.AuthResponse> refresh(@RequestBody RefreshTokenDto request) {
        return authUseCase.refreshToken(request.refreshToken());
    }

    public record LoginRequest(String identifier, String password) {
    }

    public record RefreshTokenDto(String refreshToken) {}

    public record RegisterDto(
            String username,
            String password,
            String email,
            String phone,
            String firstName,
            String lastName,
            List<RoleType> roles) {
    }
}