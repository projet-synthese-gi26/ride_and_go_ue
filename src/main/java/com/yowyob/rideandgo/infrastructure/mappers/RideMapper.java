package com.yowyob.rideandgo.infrastructure.mappers;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.RideResponse;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RideEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RideMapper {
    Ride toDomain(RideEntity entity);

    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "version", ignore = true)
    RideEntity toEntity(Ride domain);

    RideResponse toResponse(Ride domain);
}