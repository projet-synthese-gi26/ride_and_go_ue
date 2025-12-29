package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Role;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleRepositoryPort {
    Mono<Role> findRoleById(UUID roleId);

    Mono<Role> findByRoleName(RoleType type);
}
