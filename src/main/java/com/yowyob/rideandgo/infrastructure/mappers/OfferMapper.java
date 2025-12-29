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

    Offer toDomain(CreateOfferRequest request);

    OfferResponse toResponse(Offer domain);

    @Mapping(source = "interestedDrivers", target = "agreements")
    OfferEntity toEntity(Offer domain);

    default List<UUID> mapAgreementsToDriversIds(List<OfferAgreementEntity> agreements) {
        if (agreements == null || agreements.isEmpty()) return Collections.emptyList();

        return agreements.stream()
                .map(OfferAgreementEntity::getDriverId)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    default List<OfferAgreementEntity> mapDriversIdsToAgreements(List<UUID> driversIds) {
        if (driversIds == null || driversIds.isEmpty()) {
            return Collections.emptyList();
        }
        /*
        return driversIds.stream()
                .map(driverId -> {
                    OfferAgreementEntity agreement = new OfferAgreementEntity();
                    agreement.setDriverId(driverId);
                    return agreement;
                })
                .collect(Collectors.toSet());

         */

        return Collections.emptyList();
    }
}