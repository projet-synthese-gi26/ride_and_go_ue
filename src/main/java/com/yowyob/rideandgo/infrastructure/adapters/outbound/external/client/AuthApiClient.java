package com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.UUID;

@HttpExchange("/api")
public interface AuthApiClient {

        // --- AUTH ---
        @PostExchange("/auth/login")
        Mono<TraMaSysResponse> login(@RequestBody LoginRequest request);

        @PostExchange(url = "/auth/register", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
        Mono<TraMaSysResponse> register(@RequestBody MultiValueMap<String, ?> parts);

        @PostExchange("/auth/refresh")
        Mono<TraMaSysResponse> refresh(@RequestBody RefreshTokenRequest request);

        // --- USERS READ ---
        @GetExchange("/users/service/{serviceName}")
        Flux<UserDetail> getUsersByService(@PathVariable String serviceName);

        @GetExchange("/users/{id}")
        Mono<UserDetail> getUserById(@PathVariable String id);

        // --- USERS WRITE (Propagation) ---

        // Ajout de rôle (POST /api/users/{id}/roles/{roleName})
        @PostExchange("/users/{id}/roles/{roleName}")
        Mono<Void> addRole(@PathVariable String id, @PathVariable String roleName);

        // Suppression de rôle (DELETE /api/users/{id}/roles/{roleName})
        @DeleteExchange("/users/{id}/roles/{roleName}")
        Mono<Void> removeRole(@PathVariable String id, @PathVariable String roleName);

        // Mise à jour profil (PUT /api/users/{id})
        @PutExchange("/users/{id}")
        Mono<UserDetail> updateProfile(@PathVariable String id, @RequestBody UpdateProfileDto dto);

        // Changement mot de passe (PUT /api/users/{id}/password)
        @PutExchange("/users/{id}/password")
        Mono<Void> changePassword(@PathVariable String id, @RequestBody ChangePasswordDto dto);

        // --- DTOs Internes pour le Client ---

        record LoginRequest(String identifier, String password) {
        }

        record RegisterRequest(
                        String username, String password, String email, String phone,
                        String firstName, String lastName, String service, List<String> roles) {
        }

        record RefreshTokenRequest(String refreshToken) {}

        record TraMaSysResponse(String accessToken, String refreshToken, UserDetail user) {
        }

        record UserDetail(
                        String id,
                        String username,
                        String email,
                        String phone,
                        String firstName,
                        String lastName,
                        String service,
                        List<String> roles,
                        List<String> permissions,
                        UUID photoId,
                        String photoUri) {
        }

        record UpdateProfileDto(String firstName, String lastName, String phone) {
        }

        record ChangePasswordDto(String currentPassword, String newPassword) {
        }
}