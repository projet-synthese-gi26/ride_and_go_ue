package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.UserEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface UserR2dbcRepository extends ReactiveCrudRepository<UserEntity, UUID> {

    /**
     * Finds all users associated with a specific role ID using the join table.
     */
    @Query("SELECT u.* FROM users u JOIN user_has_roles uhr ON u.id = uhr.user_id WHERE uhr.role_id = :roleId")
    Flux<UserEntity> findAllByRoleId(UUID roleId);
}