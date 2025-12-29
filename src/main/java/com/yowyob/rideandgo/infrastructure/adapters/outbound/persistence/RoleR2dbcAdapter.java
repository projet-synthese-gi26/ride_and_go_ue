package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.Role;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.out.RoleRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.RoleR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.mappers.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleR2dbcAdapter implements RoleRepositoryPort {

    private final RoleR2dbcRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    public Mono<Role> findByRoleName(RoleType type) {
        return roleRepository.findByType(type)
                .map(roleMapper::toDomain);
    }

    @Override
    public Mono<Role> findRoleById(UUID roleId) {
        return roleRepository.findById(roleId)
                .map(roleMapper::toDomain);
    }
}
