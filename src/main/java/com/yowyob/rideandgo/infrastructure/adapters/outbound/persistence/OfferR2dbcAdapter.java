package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.model.Bid;
import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.ports.out.OfferRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.OfferAgreementEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.OfferEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.OfferAgreementR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.OfferR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.mappers.OfferMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferR2dbcAdapter implements OfferRepositoryPort {

    private final OfferAgreementR2dbcRepository offerAgreementRepository;
    private final OfferR2dbcRepository offerRepository;
    private final OfferMapper offerMapper;

    /**
     * Saves an Offer with proper INSERT/UPDATE detection and join table management.
     * This method is transactional to ensure atomicity between offer and bidder insertions.
     */
    @Override
    @Transactional
    public Mono<Offer> save(Offer offer) {
        OfferEntity entity = offerMapper.toEntity(offer);
        
        // STEP 1: Check if the offer already exists in the database
        return offerRepository.existsById(offer.id())
                .flatMap(exists -> {
                    // If it doesn't exist, mark it as new to force INSERT
                    if (!exists) {
                        entity.setNewEntity(true);
                    }
                    // Save the main offer entity
                    return offerRepository.save(entity);
                })
                // STEP 2: Handle bidder associations (join table)
                .flatMap(savedEntity -> {
                    // If no bids to process, return immediately
                    if (offer.bids() == null || offer.bids().isEmpty()) {
                        return Mono.just(savedEntity);
                    }

                    // For each bid, check if the driver already applied (avoid duplicates)
                    return Flux.fromIterable(offer.bids())
                            .flatMap(bid -> offerAgreementRepository
                                    .findByOfferIdAndDriverId(savedEntity.getId(), bid.driverId())
                                    // If not found, insert the new agreement
                                    .switchIfEmpty(Mono.defer(() -> offerAgreementRepository.save(
                                            new OfferAgreementEntity(
                                                Utils.generateUUID(), 
                                                bid.driverId(), 
                                                savedEntity.getId(),
                                                null, // createdDate (handled by Spring Data)
                                                null, // updatedDate (handled by Spring Data)
                                                true  // <--- CORRECTION: newEntity=true pour forcer l'INSERT
                                            )
                                    ))))
                            .collectList()
                            .map(agreements -> {
                                // Attach the persisted agreements back to the entity
                                savedEntity.setAgreements(agreements);
                                return savedEntity;
                            });
                })
                // STEP 3: Map back to domain model
                .map(this::mapToDomainManual);
    }

    /**
     * Finds an offer by ID and enriches it with its bidder associations.
     */
    @Override
    public Mono<Offer> findById(UUID offerId) {
        return offerRepository.findById(offerId)
                .flatMap(this::enrichOfferWithAgreements)
                .map(this::mapToDomainManual);
    }

    /**
     * Lists all offers with their bidder data.
     */
    @Override
    public Flux<Offer> findAll() {
        return offerRepository.findAll()
                .flatMap(this::enrichOfferWithAgreements)
                .map(this::mapToDomainManual);
    }

    /**
     * Helper method to fetch bidder linkages from the join table.
     */
    private Mono<OfferEntity> enrichOfferWithAgreements(OfferEntity entity) {
        return offerAgreementRepository.findByOfferId(entity.getId())
                .collectList()
                .map(agreements -> {
                    entity.setAgreements(agreements);
                    return entity;
                });
    }

    /**
     * Manual mapping from Entity to Domain because MapStruct doesn't handle
     * the bidder list properly (it's stored in a separate table).
     */
    private Offer mapToDomainManual(OfferEntity entity) {
        Offer domain = offerMapper.toDomain(entity);
        
        // Convert SQL agreements to domain Bid objects
        if (entity.getAgreements() != null) {
            return domain.withBids(entity.getAgreements().stream()
                    .map(a -> Bid.builder().driverId(a.getDriverId()).build())
                    .collect(Collectors.toList()));
        }
        return domain;
    }

    @Override
    public Mono<Boolean> delete(Offer offer) {
        return offerRepository.delete(offerMapper.toEntity(offer))
                .thenReturn(true)
                .onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> exists(Offer offer) {
        return offerRepository.existsById(offer.id());
    }
}