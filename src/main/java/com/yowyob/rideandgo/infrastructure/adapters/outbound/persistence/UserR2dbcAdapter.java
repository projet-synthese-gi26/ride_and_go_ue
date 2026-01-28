package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.Role;
import com.yowyob.rideandgo.domain.model.Permission;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.UserEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.*;
import com.yowyob.rideandgo.infrastructure.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RoleEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.r2dbc.core.DatabaseClient;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserR2dbcAdapter implements UserRepositoryPort {

    private final UserR2dbcRepository userRepository;
    private final RoleR2dbcRepository roleRepository;
    private final PermissionR2dbcRepository permissionRepository;
    private final UserMapper userMapper;
    private final DatabaseClient databaseClient;

    private static final String SCHEMA = "ride_and_go"; // Nom du schéma

    @Override
    public Mono<User> findUserById(UUID userId) {
        return userRepository.findById(userId)
                .flatMap(this::enrichUser);
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll()
                .flatMap(this::enrichUser);
    }

    @Override
    public Flux<User> findByRoleName(RoleType type) {
        return roleRepository.findByName(type)
                .flatMap(role -> userRepository.findAllByRoleId(role.getId()))
                .flatMap(this::enrichUser);
    }

    @Override
    @Transactional
    public Mono<User> save(User user) {
        UserEntity entity = userMapper.toEntity(user);

        return userRepository.existsById(user.id())
                .flatMap(exists -> {
                    if (!exists)
                        entity.setNewEntity(true);
                    return userRepository.save(entity);
                })
                .flatMap(savedEntity -> syncRoles(user, savedEntity))
                .flatMap(savedEntity -> syncActorTables(user, savedEntity))
                .map(savedEntity -> user);
    }

    /**
     * Insère l'utilisateur dans la table 'customers' ou 'drivers' selon son rôle.
     * C'est nécessaire pour satisfaire les FK des autres tables (offers, rides).
     * CORRECTION: Ajout du préfixe de schéma.
     */
    private Mono<UserEntity> syncActorTables(User user, UserEntity savedEntity) {
        if (user.roles() == null || user.roles().isEmpty())
            return Mono.just(savedEntity);

        return Flux.fromIterable(user.roles())
                .flatMap(role -> {
                    String roleName = role.type().name();

                    if (roleName.contains("PASSENGER") || roleName.contains("CUSTOMER")) {
                        String sql = String.format(
                                "INSERT INTO %s.customers (id, code, payment_method) VALUES (:id, :code, 'CASH') ON CONFLICT DO NOTHING",
                                SCHEMA);
                        return databaseClient.sql(sql)
                                .bind("id", savedEntity.getId())
                                .bind("code", "CUST-" + savedEntity.getId().toString().substring(0, 8))
                                .then();
                    } else if (roleName.contains("DRIVER")) {
                        // Pour un driver, il faut d'abord l'insérer dans 'business_actors' puis dans
                        // 'drivers'.
                        String sqlBA = String.format(
                                "INSERT INTO %s.business_actors (id, name, email_address, phone_number) VALUES (:id, :name, :email, :phone) ON CONFLICT DO NOTHING",
                                SCHEMA);
                        String sqlDriver = String.format(
                                "INSERT INTO %s.drivers (id, status, license_number) VALUES (:id, 'AVAILABLE', 'UNKNOWN') ON CONFLICT DO NOTHING",
                                SCHEMA);

                        return databaseClient.sql(sqlBA)
                                .bind("id", savedEntity.getId())
                                .bind("name", savedEntity.getName())
                                .bind("email", savedEntity.getEmail())
                                .bind("phone", savedEntity.getTelephone())
                                .then()
                                .then(databaseClient.sql(sqlDriver)
                                        .bind("id", savedEntity.getId())
                                        .then());
                    }
                    return Mono.empty();
                })
                .then(Mono.just(savedEntity));
    }

    @Override
    public Mono<Boolean> delete(User user) {
        return userRepository.deleteById(user.id()).thenReturn(true).onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> deleteById(UUID userId) {
        return userRepository.deleteById(userId).thenReturn(true).onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> exists(User user) {
        return userRepository.existsById(user.id());
    }

    // --- NOUVELLE IMPLEMENTATION : ADD ROLE ---
    @Override
    public Mono<Void> addRoleToUser(UUID userId, RoleType roleType) {
        return roleRepository.findByName(roleType).next()
                .flatMap(roleEntity -> {
                    String sql = String.format(
                            "INSERT INTO %s.user_has_roles (user_id, role_id) VALUES (:userId, :roleId) ON CONFLICT DO NOTHING",
                            SCHEMA);
                    return databaseClient.sql(sql)
                            .bind("userId", userId)
                            .bind("roleId", roleEntity.getId())
                            .then();
                });
    }

    private Mono<User> enrichUser(UserEntity entity) {
        // Fetch Roles and their specific permissions
        Mono<Set<Role>> rolesMono = roleRepository.findAllByUserId(entity.getId())
                .flatMap(roleEntity -> permissionRepository.findAllByRoleId(roleEntity.getId())
                        .map(p -> new Permission(p.getId(), p.getName()))
                        .collect(Collectors.toSet())
                        .map(perms -> Role.builder()
                                .id(roleEntity.getId())
                                .type(roleEntity.getName())
                                .permissions(perms)
                                .build()))
                .collect(Collectors.toSet());

        // Fetch permissions assigned directly to the user
        Mono<Set<Permission>> directPermsMono = permissionRepository.findDirectPermissionsByUserId(entity.getId())
                .map(p -> new Permission(p.getId(), p.getName()))
                .collect(Collectors.toSet());

        // Combine all reactive streams
        return Mono.zip(rolesMono, directPermsMono)
                .map(tuple -> User.builder()
                        .id(entity.getId())
                        .name(entity.getName())
                        .email(entity.getEmail())
                        .telephone(entity.getTelephone())
                        .roles(tuple.getT1())
                        .directPermissions(tuple.getT2())
                        .build());
    }

    /**
     * Méthode helper pour gérer la table de liaison user_has_roles
     */
    private Mono<UserEntity> syncRoles(User domainUser, UserEntity savedEntity) {
        if (domainUser.roles() == null || domainUser.roles().isEmpty()) {
            return Mono.just(savedEntity);
        }

        String sqlDelete = String.format("DELETE FROM %s.user_has_roles WHERE user_id = :userId", SCHEMA);

        return databaseClient.sql(sqlDelete)
                .bind("userId", savedEntity.getId())
                .then()
                .thenMany(Flux.fromIterable(domainUser.roles()))
                .flatMap(domainRole -> roleRepository.findByName(domainRole.type()).next()
                        .switchIfEmpty(Mono.defer(() -> {
                            RoleEntity newRole = new RoleEntity(
                                    java.util.UUID.randomUUID(),
                                    domainRole.type(),
                                    LocalDateTime.now(),
                                    LocalDateTime.now(),
                                    true);
                            return roleRepository.save(newRole);
                        }))
                        .flatMap(roleEntity -> {
                            String sqlInsert = String.format(
                                    "INSERT INTO %s.user_has_roles (user_id, role_id) VALUES (:userId, :roleId) ON CONFLICT DO NOTHING",
                                    SCHEMA);
                            return databaseClient.sql(sqlInsert)
                                    .bind("userId", savedEntity.getId())
                                    .bind("roleId", roleEntity.getId())
                                    .then();
                        }))
                .then(Mono.just(savedEntity));
    }
}