package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.ports.out.ExternalUserPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.AuthApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Collections;
import java.util.UUID;
import com.yowyob.rideandgo.domain.model.Role;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import java.util.stream.Collectors;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class RemoteUserAdapter implements ExternalUserPort {
    private final AuthApiClient client;
    private static final String SERVICE_NAME = "RIDE_AND_GO";

    // --- READ ---

    @Override
    public Flux<User> fetchAllRemoteUsers() {
        return getUsersByService(SERVICE_NAME);
    }

    @Override
    public Flux<User> fetchAllRemoteUsersByService(String serviceName) {
        return getUsersByService(serviceName);
    }

    private Flux<User> getUsersByService(String service) {
        return client.getUsersByService(service).map(this::mapToDomain);
    }

    @Override
    public Mono<User> fetchRemoteUserById(UUID id) {
        return client.getUserById(id.toString())
                .map(this::mapToDomain)
                .onErrorResume(e -> {
                    log.warn("User {} not found via direct ID call, trying list filter...", id);
                    return fetchAllRemoteUsers()
                            .filter(u -> u.id().equals(id))
                            .next();
                });
    }

    // --- WRITE (Propagation) ---

    @Override
    public Mono<Void> addRole(UUID userId, String roleName) {
        log.info("🌍 Propagating ADD ROLE {} for user {} to Auth Service", roleName, userId);
        return client.addRole(userId.toString(), roleName);
    }

    @Override
    public Mono<Void> removeRole(UUID userId, String roleName) {
        log.info("🌍 Propagating REMOVE ROLE {} for user {} to Auth Service", roleName, userId);
        return client.removeRole(userId.toString(), roleName);
    }

    @Override
    public Mono<User> updateProfile(UUID userId, String firstName, String lastName, String phone) {
        log.info("🌍 Propagating UPDATE PROFILE for user {} to Auth Service", userId);
        return client.updateProfile(userId.toString(), new AuthApiClient.UpdateProfileDto(firstName, lastName, phone))
                .map(this::mapToDomain);
    }

    @Override
    public Mono<Void> changePassword(UUID userId, String currentPassword, String newPassword) {
        log.info("🌍 Propagating PASSWORD CHANGE for user {} to Auth Service", userId);
        return client.changePassword(userId.toString(),
                new AuthApiClient.ChangePasswordDto(currentPassword, newPassword));
    }

    // --- MAPPER ---

    private User mapToDomain(AuthApiClient.UserDetail dto) {
        Set<Role> roles = dto.roles() != null ? dto.roles().stream()
                .filter(roleStr -> {
                    try {
                        RoleType.valueOf(roleStr);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .map(roleStr -> Role.builder().type(RoleType.valueOf(roleStr)).build())
                .collect(Collectors.toSet()) : Collections.emptySet();

        return User.builder()
                .id(UUID.fromString(dto.id()))
                .name(dto.username()) // Username est utilisé comme name ici, ou on concatène First/Last
                .email(dto.email())
                .telephone(dto.phone())
                .roles(roles)
                .directPermissions(Collections.emptySet())
                .build();
    }
}