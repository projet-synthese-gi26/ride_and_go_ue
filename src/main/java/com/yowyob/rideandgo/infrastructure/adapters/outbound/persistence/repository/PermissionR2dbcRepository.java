package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.PermissionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;

public interface PermissionR2dbcRepository extends ReactiveCrudRepository<PermissionEntity, UUID> {
    
    @Query("SELECT p.* FROM permissions p JOIN role_has_permissions rhp ON p.id = rhp.permission_id WHERE rhp.role_id = :roleId")
    Flux<PermissionEntity> findAllByRoleId(UUID roleId);

    @Query("SELECT p.* FROM permissions p JOIN user_has_permissions uhp ON p.id = uhp.permission_id WHERE uhp.user_id = :userId")
    Flux<PermissionEntity> findDirectPermissionsByUserId(UUID userId);
}