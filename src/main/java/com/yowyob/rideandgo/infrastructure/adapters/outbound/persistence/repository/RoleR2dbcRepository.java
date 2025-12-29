package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RoleEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleR2dbcRepository extends ReactiveCrudRepository<RoleEntity, UUID> {
    
    /**
     * Finds a role entity by its business type (Enum).
     */
    Mono<RoleEntity> findByType(RoleType type);

    /**
     * Finds all roles associated with a user through the join table.
     */
    @Query("SELECT r.* FROM roles r JOIN user_has_roles uhr ON r.id = uhr.role_id WHERE uhr.user_id = :userId")
    Flux<RoleEntity> findAllByUserId(UUID userId);
}