package com.yowyob.rideandgo.infrastructure.mappers;

import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.CreateOfferRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.OfferResponse;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.OfferAgreementEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.OfferEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OfferMapper {

    @Mapping(source = "agreements", target = "interestedDrivers")
    Offer toDomain(OfferEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "interestedDrivers", ignore = true)
    @Mapping(target = "version", ignore = true)
    Offer toDomain(CreateOfferRequest request);

    OfferResponse toResponse(Offer domain);

    // Fix: We ignore 'agreements' here because the persistence adapter 
    // handles the saving of links in the 'offer_driver_linkages' table separately.
    @Mapping(target = "agreements", ignore = true) 
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    OfferEntity toEntity(Offer domain);

    /**
     * Helper to map list of entities to list of UUIDs (Entity -> Domain)
     */
    default List<UUID> mapAgreementsToDriversIds(List<OfferAgreementEntity> agreements) {
        if (agreements == null || agreements.isEmpty()) {
            return Collections.emptyList();
        }
        return agreements.stream()
                .map(OfferAgreementEntity::getDriverId)
                .collect(Collectors.toList());
    }
}