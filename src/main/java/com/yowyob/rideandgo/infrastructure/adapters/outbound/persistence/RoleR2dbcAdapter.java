package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.Role;
import com.yowyob.rideandgo.domain.model.Permission;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.out.RoleRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RoleEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.PermissionR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.RoleR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleR2dbcAdapter implements RoleRepositoryPort {

    private final RoleR2dbcRepository roleRepository;
    private final PermissionR2dbcRepository permissionRepository;

    @Override
    public Mono<Role> findByRoleName(RoleType type) {
        // ✅ CORRECTION : Appel de findByName
        return roleRepository.findByName(type)
                .flatMap(this::enrichRole);
    }

    @Override
    public Mono<Role> findRoleById(UUID roleId) {
        return roleRepository.findById(roleId)
                .flatMap(this::enrichRole);
    }

    private Mono<Role> enrichRole(RoleEntity entity) {
        return permissionRepository.findAllByRoleId(entity.getId())
                .map(p -> new Permission(p.getId(), p.getName()))
                .collect(Collectors.toSet())
                .map(perms -> Role.builder()
                        .id(entity.getId())
                        // ✅ CORRECTION : entity.getType() -> entity.getName()
                        .type(entity.getName()) 
                        .permissions(perms)
                        .build());
    }
}