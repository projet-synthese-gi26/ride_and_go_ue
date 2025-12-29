package com.yowyob.rideandgo.infrastructure.mappers;

import com.yowyob.rideandgo.domain.model.Fare;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FareMapper {

    @Mapping(target = "startPoint", source = "startLocationName")
    @Mapping(target = "endPoint", source = "endLocationName")
    Fare toDomain(FareResponse response);

    @Mapping(target = "startLocationName", source = "domain.startPoint")
    @Mapping(target = "endLocationName", source = "domain.endPoint")
    @Mapping(target = "cached", source = "isCached")
    FareResponse toResponse(Fare domain, boolean isCached);

    @Mapping(target = "startPoint", source = "startLocationName")
    @Mapping(target = "endPoint", source = "endLocationName")
    Fare toDomain(FareRequest request);
}