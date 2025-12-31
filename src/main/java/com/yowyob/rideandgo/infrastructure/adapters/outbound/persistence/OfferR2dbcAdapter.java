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

    @Override
    @Transactional
    public Mono<Offer> save(Offer offer) {
        OfferEntity entity = offerMapper.toEntity(offer);
        
        return offerRepository.existsById(offer.id())
                .flatMap(exists -> {
                    if (!exists) {
                        entity.setNewEntity(true); // Force INSERT
                    }
                    return offerRepository.save(entity);
                })
                .flatMap(savedEntity -> {
                    // OPTIMISATION : Si la liste des bids est vide (ex: création) ou null, on ne fait rien sur la table de liaison.
                    // Si on est dans un Update de statut (ex: SELECT_DRIVER), on ne touche pas aux bids non plus ici, 
                    // car ils ont été insérés lors du 'Apply'.
                    // On ne sauvegarde les bids que si c'est explicitement demandé par le cas d'usage (ex: ResponseToOffer).
                    
                    if (offer.bids() == null || offer.bids().isEmpty()) {
                        return Mono.just(savedEntity);
                    }

                    // On ne traite les bids que s'ils ne sont pas déjà liés.
                    // Pour éviter les conflits SQL lors d'un update global, on utilise insert-ignore logique via 'switchIfEmpty'
                    return Flux.fromIterable(offer.bids())
                            .flatMap(bid -> offerAgreementRepository
                                    .findByOfferIdAndDriverId(savedEntity.getId(), bid.driverId())
                                    .switchIfEmpty(Mono.defer(() -> offerAgreementRepository.save(
                                            new OfferAgreementEntity(
                                                Utils.generateUUID(), 
                                                bid.driverId(), 
                                                savedEntity.getId(),
                                                null, 
                                                null, 
                                                true // Force INSERT
                                            )
                                    ))))
                            .collectList()
                            .map(agreements -> {
                                savedEntity.setAgreements(agreements);
                                return savedEntity;
                            });
                })
                .map(this::mapToDomainManual);
    }

    @Override
    public Mono<Offer> findById(UUID offerId) {
        return offerRepository.findById(offerId)
                .flatMap(this::enrichOfferWithAgreements)
                .map(this::mapToDomainManual);
    }

    @Override
    public Flux<Offer> findAll() {
        return offerRepository.findAll()
                .flatMap(this::enrichOfferWithAgreements)
                .map(this::mapToDomainManual);
    }

    private Mono<OfferEntity> enrichOfferWithAgreements(OfferEntity entity) {
        return offerAgreementRepository.findByOfferId(entity.getId())
                .collectList()
                .map(agreements -> {
                    entity.setAgreements(agreements);
                    return entity;
                });
    }

    private Offer mapToDomainManual(OfferEntity entity) {
        Offer domain = offerMapper.toDomain(entity);
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