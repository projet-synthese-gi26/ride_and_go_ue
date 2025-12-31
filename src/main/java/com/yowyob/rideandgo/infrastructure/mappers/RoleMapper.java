package com.yowyob.rideandgo.infrastructure.mappers;

import com.yowyob.rideandgo.domain.model.Role;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {

    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "type", source = "name") // IMPORTANT: Entity.name -> Domain.type
    Role toDomain(RoleEntity entity);

    @Mapping(target = "name", source = "type") // IMPORTANT: Domain.type -> Entity.name
    RoleEntity toEntity(Role domain);
}