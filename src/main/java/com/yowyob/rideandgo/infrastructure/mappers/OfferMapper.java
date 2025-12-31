package com.yowyob.rideandgo.infrastructure.mappers;

import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.CreateOfferRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.OfferResponse;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.OfferEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfferMapper {

    @Mapping(target = "bids", ignore = true)
    @Mapping(target = "version", ignore = true) 
    Offer toDomain(OfferEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bids", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "selectedDriverId", ignore = true) // Pas de driver à la création
    Offer toDomain(CreateOfferRequest request);

    // MapStruct mappera automatiquement selectedDriverId -> selectedDriverId
    OfferResponse toResponse(Offer domain);

    @Mapping(target = "agreements", ignore = true) 
    OfferEntity toEntity(Offer domain);
}