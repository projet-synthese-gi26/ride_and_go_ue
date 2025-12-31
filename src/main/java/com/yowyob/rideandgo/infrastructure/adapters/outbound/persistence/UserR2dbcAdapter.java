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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                .flatMapMany(role -> userRepository.findAllByRoleId(role.getId()))
                .flatMap(this::enrichUser);
    }

    @Override
    public Mono<User> save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        return userRepository.save(entity)
                .map(saved -> user); 
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

    /**
     * Aggregates Roles and Permissions into a complete Domain User object.
     * Uses parallel fetching to optimize performance.
     */
    private Mono<User> enrichUser(UserEntity entity) {
        // Fetch Roles and their specific permissions
        Mono<Set<Role>> rolesMono = roleRepository.findAllByUserId(entity.getId())
                .flatMap(roleEntity -> 
                    permissionRepository.findAllByRoleId(roleEntity.getId())
                        .map(p -> new Permission(p.getId(), p.getName()))
                        .collect(Collectors.toSet())
                        .map(perms -> Role.builder()
                                .id(roleEntity.getId())
                                .type(roleEntity.getName())
                                .permissions(perms)
                                .build())
                ).collect(Collectors.toSet());

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
}