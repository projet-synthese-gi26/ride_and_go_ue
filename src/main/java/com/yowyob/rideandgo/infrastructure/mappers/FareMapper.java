package com.yowyob.rideandgo.infrastructure.mappers;

import com.yowyob.rideandgo.domain.model.Fare;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FareMapper {

    /**
     * Maps API response to domain model.
     */
    @Mapping(target = "startPoint", source = "startLocationName")
    @Mapping(target = "endPoint", source = "endLocationName")
    @Mapping(target = "userId", ignore = true)
    Fare toDomain(FareResponse response);

    /**
     * Maps domain model to API response for the frontend.
     */
    @Mapping(target = "startLocationName", source = "domain.startPoint")
    @Mapping(target = "endLocationName", source = "domain.endPoint")
    @Mapping(target = "cached", source = "isCached")
    FareResponse toResponse(Fare domain, boolean isCached);

    /**
     * Maps initial request to domain model.
     */
    @Mapping(target = "startPoint", source = "startLocationName")
    @Mapping(target = "endPoint", source = "endLocationName")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "estimatedFare", ignore = true)
    @Mapping(target = "officialFare", ignore = true)
    Fare toDomain(FareRequest request);
}   