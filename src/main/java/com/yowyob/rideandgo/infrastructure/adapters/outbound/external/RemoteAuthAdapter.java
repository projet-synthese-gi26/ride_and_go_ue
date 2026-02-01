package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.exception.AuthenticationFailedException;
import com.yowyob.rideandgo.domain.exception.UserAlreadyExistsException;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import com.yowyob.rideandgo.domain.ports.out.CacheInvalidationPort;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.AuthApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
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
    private final UserRepositoryPort userRepositoryPort;
    private final CacheInvalidationPort cacheInvalidationPort;

    private static final String SERVICE_NAME = "RIDE_AND_GO";

    @Override
    public Mono<AuthResponse> login(String identifier, String password) {
        log.info("🌐 REMOTE AUTH : Login pour {}", identifier);
        return client.login(new AuthApiClient.LoginRequest(identifier, password))
                .doOnSuccess(response -> {
                    if (response != null && response.user() != null && response.user().id() != null) {
                        UUID userId = UUID.fromString(response.user().id());
                        cacheInvalidationPort.invalidateUserCache(userId).subscribe();
                    }
                })
                .map(this::mapToDomain)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        return Mono.error(new AuthenticationFailedException("Identifiant ou mot de passe incorrect."));
                    }
                    return Mono.error(new RuntimeException("Erreur d'authentification distante"));
                });
    }

    @Override
    public Mono<AuthResponse> register(String username, String email, String password, String phone,
            String firstName, String lastName, List<RoleType> roles, FilePart photo) {
        
        log.info("🌐 REMOTE AUTH : Register Multipart avec photo pour {}", username);

        List<String> rolesToSend = roles.stream().map(Enum::name).toList();

        AuthApiClient.RegisterRequest registerDto = new AuthApiClient.RegisterRequest(
                username, password, email, phone, firstName, lastName, SERVICE_NAME, rolesToSend);

        // Construction du corps Multipart
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("data", registerDto, MediaType.APPLICATION_JSON);
        
        if (photo != null) {
            builder.part("file", photo);
        }

        return client.register(builder.build())
                .flatMap(response -> {
                    // Mapping vers l'utilisateur local incluant la photoUri reçue
                    User localUser = User.builder()
                            .id(UUID.fromString(response.user().id()))
                            .name(response.user().username())
                            .email(response.user().email())
                            .telephone(response.user().phone())
                            .photoUri(response.user().photoUri()) // ✅ PHOTO STOCKÉE
                            .roles(Collections.emptySet())
                            .directPermissions(Collections.emptySet())
                            .build();

                    return userRepositoryPort.save(localUser)
                            .doOnSuccess(u -> log.info("✅ User synced locally with photo: {}", u.photoUri()))
                            .thenReturn(mapToDomain(response));
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("❌ Register Failed (Remote): {}", ex.getStatusCode());
                    if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                        return Mono.error(new UserAlreadyExistsException("L'utilisateur existe déjà."));
                    }
                    return Mono.error(new RuntimeException("Erreur inscription : " + ex.getResponseBodyAsString()));
                });
    }

    @Override
    public Mono<AuthResponse> refreshToken(String refreshToken) {
        return client.refresh(new AuthApiClient.RefreshTokenRequest(refreshToken))
                .doOnSuccess(response -> {
                    if (response != null && response.user() != null && response.user().id() != null) {
                        cacheInvalidationPort.invalidateUserCache(UUID.fromString(response.user().id())).subscribe();
                    }
                })
                .map(this::mapToDomain);
    }

    @Override
    public Mono<Void> forgotPassword(String email) { return Mono.empty(); }

    private AuthResponse mapToDomain(AuthApiClient.TraMaSysResponse res) {
        List<String> filteredRoles = res.user().roles().stream()
                .filter(roleStr -> {
                    try { RoleType.valueOf(roleStr); return true; } catch (Exception e) { return false; }
                }).collect(Collectors.toList());

        return new AuthResponse(
                UUID.fromString(res.user().id()),
                res.accessToken(), res.refreshToken(),
                res.user().username(), filteredRoles, res.user().permissions());
    }
}