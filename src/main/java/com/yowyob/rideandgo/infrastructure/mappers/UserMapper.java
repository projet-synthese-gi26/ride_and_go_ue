package com.yowyob.rideandgo.infrastructure.mappers;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.CreateUserRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UserResponse;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    /**
     * Maps database entity to domain model.
     * Roles and permissions are handled manually in the Persistence Adapter.
     */
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "directPermissions", ignore = true)
    User toDomain(UserEntity entity);

    /**
     * Maps domain model to database entity.
     * Join tables are handled separately in the persistence layer.
     */
    UserEntity toEntity(User domain);

    /**
     * Maps registration request to domain model.
     */
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "directPermissions", ignore = true)
    User toDomain(CreateUserRequest request);

    /**
     * Maps domain model to API response.
     * The specific RoleType is manually set in the Controller.
     */
    @Mapping(target = "role", ignore = true)
    UserResponse toResponse(User domain);
}