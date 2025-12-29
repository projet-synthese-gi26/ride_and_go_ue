package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RoleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleR2dbcRepository extends ReactiveCrudRepository<RoleEntity, UUID> {
    Mono<RoleEntity> findByType(RoleType type);
}
