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

    /**
     * Mappe la requête de création vers le domaine.
     * passengerPhone et departureTime sont mappés automatiquement par nom.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bids", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "passengerId", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "selectedDriverId", ignore = true)
    Offer toDomain(CreateOfferRequest request);

    OfferResponse toResponse(Offer domain);

    @Mapping(target = "agreements", ignore = true)
    OfferEntity toEntity(Offer domain);
}