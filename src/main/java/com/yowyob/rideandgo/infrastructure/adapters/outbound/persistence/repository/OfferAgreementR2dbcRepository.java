package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.OfferAgreementEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OfferAgreementR2dbcRepository extends ReactiveCrudRepository<OfferAgreementEntity, UUID> {
    Mono<OfferAgreementEntity> findByOfferIdAndDriverId(UUID offerId, UUID driverId);

    Flux<OfferAgreementEntity> findByOfferId(UUID offerId);
}
