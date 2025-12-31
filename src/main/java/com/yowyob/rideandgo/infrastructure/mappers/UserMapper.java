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

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "directPermissions", ignore = true)
    User toDomain(UserEntity entity);

    UserEntity toEntity(User domain);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "directPermissions", ignore = true)
    User toDomain(CreateUserRequest request);

    @Mapping(target = "role", ignore = true)
    UserResponse toResponse(User domain);
}