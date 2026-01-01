package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.AuthApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RemoteAuthAdapter implements AuthPort {

    private final AuthApiClient client;
    private final UserRepositoryPort userRepository;

    private static final String SERVICE_NAME = "RIDE_AND_GO";

    @Override
    public Mono<AuthResponse> login(String identifier, String password) {
        log.info("🌐 REMOTE AUTH : Login pour {}", identifier);
        return client.login(new AuthApiClient.LoginRequest(identifier, password))
                .map(this::mapToDomain)
                .doOnError(e -> log.error("Login failed: {}", e.getMessage()));
    }

    @Override
    public Mono<AuthResponse> register(String username, String email, String password, String phone, String firstName, String lastName, List<RoleType> roles) {
        log.info("🌐 REMOTE AUTH : Register pour {} avec rôles {}", username, roles);

        // Transformation dynamique : List<Enum> -> List<String>
        List<String> rolesToSend = roles.stream()
                .map(Enum::name)
                .toList();

        AuthApiClient.RegisterRequest request = new AuthApiClient.RegisterRequest(
            username, password, email, phone, firstName, lastName,
            SERVICE_NAME, 
            rolesToSend
        );

        return client.register(request)
                .flatMap(response -> {
                    // Sauvegarde locale (ID, Info, mais pas les rôles car gérés à distance)
                    User localUser = User.builder()
                            .id(UUID.fromString(response.user().id()))
                            .name(response.user().username())
                            .email(response.user().email())
                            .telephone(response.user().phone())
                            .roles(Collections.emptySet())
                            .directPermissions(Collections.emptySet())
                            .build();

                    return userRepository.save(localUser)
                            .doOnSuccess(u -> log.info("✅ User synced locally: {}", u.id()))
                            .thenReturn(mapToDomain(response));
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("❌ Register Failed (Remote): {}", ex.getResponseBodyAsString());
                    return Mono.error(new RuntimeException("Erreur inscription : " + ex.getResponseBodyAsString()));
                });
    }

    @Override
    public Mono<Void> forgotPassword(String email) {
        return Mono.empty(); 
    }

    private AuthResponse mapToDomain(AuthApiClient.TraMaSysResponse res) {
        List<String> filteredRoles = res.user().roles().stream()
                .filter(roleStr -> {
                    try {
                        RoleType.valueOf(roleStr);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        return new AuthResponse(
            res.accessToken(),
            res.refreshToken(),
            res.user().username(),
            filteredRoles,
            res.user().permissions()
        );
    }
}