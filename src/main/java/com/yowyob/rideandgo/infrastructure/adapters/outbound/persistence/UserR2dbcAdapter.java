package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.RoleR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.UserR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.mappers.RoleMapper;
import com.yowyob.rideandgo.infrastructure.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserR2dbcAdapter implements UserRepositoryPort {

    private final UserR2dbcRepository userRepository;
    private final UserMapper userMapper;
    private final RoleR2dbcRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    public Mono<User> save(User user) {
        return userRepository.save(userMapper.toEntity(user))
                .flatMap(userEntity -> userMapper.toDomain(userEntity, roleRepository, roleMapper));
    }

    @Override
    public Mono<Boolean> delete(User user) {
        return userRepository.deleteById(user.id())
                .thenReturn(true);
    }

    @Override
    public Mono<Boolean> deleteById(UUID userId) {
        return userRepository.deleteById(userId)
                .thenReturn(true);
    }

    @Override
    public Mono<Boolean> exists(User user) {
        return userRepository.existsById(user.id());
    }

    @Override
    public Mono<User> findUserById(UUID userId) {
        return userRepository.findById(userId)
                .flatMap(entity -> userMapper.toDomain(entity, roleRepository, roleMapper));
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll()
                .flatMap(entity -> userMapper.toDomain(entity, roleRepository, roleMapper));
    }

    @Override
    public Flux<User> findByRoleName(RoleType type) {
        return roleRepository.findByType(type)
                .flatMapMany(role ->
                        userRepository.findAllByRoleId(role.getId())
                                .flatMap(entity -> userMapper.toDomain(entity, roleRepository, roleMapper))
                );
    }

    @Override
    public List<User> findByRoleNameList(RoleType type) {
        return findByRoleName(type).collectList().block();
    }
}
