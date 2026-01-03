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

    @Override
    public Flux<User> fetchAllRemoteUsers() {
        return client.getUsersByService(SERVICE_NAME).map(this::mapToDomain);
    }

    @Override
    public Mono<User> fetchRemoteUserById(UUID id) {
        // STRATÉGIE DE CONTOURNEMENT :
        // La route directe /users/{id} renvoie 401.
        // On utilise la route de liste (qui marche) et on filtre nous-même.
        return client.getUsersByService(SERVICE_NAME)
                .filter(u -> u.id().equals(id.toString())) // On cherche l'utilisateur dans la liste
                .next() // On prend le premier (Mono)
                .map(this::mapToDomain)
                // Si on ne le trouve pas dans la liste, on renvoie empty
                .switchIfEmpty(Mono.empty());
    }

    private User mapToDomain(AuthApiClient.UserDetail dto) {
        // 1. Conversion des rôles String -> Domain Role
        Set<Role> roles = dto.roles().stream()
                .filter(roleStr -> {
                    try {
                        // On filtre pour ne garder que les rôles connus de notre Enum
                        RoleType.valueOf(roleStr);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false; 
                    }
                })
                .map(roleStr -> Role.builder()
                        .type(RoleType.valueOf(roleStr))
                        // On ne connait pas l'ID du rôle ici, c'est la persistence qui le trouvera via le type
                        .build()) 
                .collect(Collectors.toSet());

        return User.builder()
                .id(UUID.fromString(dto.id()))
                .name(dto.username())
                .email(dto.email())
                .telephone(dto.phone())
                .roles(roles) // <--- ON PASSE LES RÔLES ICI
                .directPermissions(Collections.emptySet())
                .build();
    }
}