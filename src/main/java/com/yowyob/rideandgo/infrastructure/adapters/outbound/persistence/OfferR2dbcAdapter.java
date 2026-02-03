package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.model.Bid;
import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.model.enums.OfferState;
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

import java.util.Collections;
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
        log.info("💾 SAVE OFFER: ID={}, Bids Count={}", offer.id(),
                (offer.bids() != null ? offer.bids().size() : "null"));

        OfferEntity entity = offerMapper.toEntity(offer);

        return offerRepository.existsById(offer.id())
                .flatMap(exists -> {
                    if (!exists)
                        entity.setNewEntity(true);
                    return offerRepository.save(entity);
                })
                .flatMap(savedOffer -> {
                    if (offer.bids() == null || offer.bids().isEmpty()) {
                        log.info("⚠️ No bids to save for offer {}", offer.id());
                        return Mono.just(savedOffer);
                    }

                    return Flux.fromIterable(offer.bids())
                            .flatMap(bid -> {
                                log.debug("Processing bid for driver {}", bid.driverId());
                                return offerAgreementRepository
                                        .findByOfferIdAndDriverId(savedOffer.getId(), bid.driverId())
                                        .doOnNext(
                                                found -> log.debug("Bid already exists for driver {}", bid.driverId()))
                                        .switchIfEmpty(
                                                Mono.defer(() -> {
                                                    log.info("➕ INSERTING BID: Driver {} -> Offer {}", bid.driverId(),
                                                            savedOffer.getId());
                                                    OfferAgreementEntity link = new OfferAgreementEntity();
                                                    link.setId(Utils.generateUUID());
                                                    link.setOfferId(savedOffer.getId());
                                                    link.setDriverId(bid.driverId());
                                                    link.asNew();
                                                    return offerAgreementRepository.save(link)
                                                            .doOnSuccess(
                                                                    s -> log.info("✅ Inserted link ID: {}", s.getId()))
                                                            .doOnError(e -> log.error("❌ Failed to insert link: {}",
                                                                    e.getMessage()));
                                                }));
                            })
                            .collectList() // Force l'exécution
                            .map(links -> {
                                log.info("🔄 Processed {} links", links.size());
                                return savedOffer;
                            });
                })
                .flatMap(savedOffer -> {
                    // Force reload pour vérifier ce qui a été persisté
                    return findById(savedOffer.getId())
                            .doOnSuccess(o -> log.info("🔍 Reloaded Offer: Bids Count={}",
                                    (o.bids() != null ? o.bids().size() : 0)));
                });
    }

    @Override
    public Flux<Offer> findLatestPending(int limit) {
        return offerRepository.findAll()
                .filter(o -> o.getState() == OfferState.PENDING || o.getState() == OfferState.BID_RECEIVED)
                .sort((o1, o2) -> o2.getCreatedDate().compareTo(o1.getCreatedDate()))
                .take(limit)
                .flatMap(this::enrichOfferWithAgreements)
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
                    entity.setAgreements(agreements != null ? agreements : Collections.emptyList());
                    // Log debug pour voir si on récupère bien de la base
                    // if (!entity.getAgreements().isEmpty()) log.debug("DB returned {} agreements
                    // for offer {}", entity.getAgreements().size(), entity.getId());
                    return entity;
                });
    }

    private Offer mapToDomainManual(OfferEntity entity) {
        Offer domain = offerMapper.toDomain(entity);
        if (entity.getAgreements() != null && !entity.getAgreements().isEmpty()) {
            return domain.withBids(entity.getAgreements().stream()
                    .map(a -> Bid.builder().driverId(a.getDriverId()).build())
                    .collect(Collectors.toList()));
        } else {
            return domain.withBids(Collections.emptyList());
        }
    }

    @Override
    public Mono<Boolean> delete(Offer offer) {
        return offerRepository.delete(offerMapper.toEntity(offer)).thenReturn(true);
    }

    @Override
    public Mono<Boolean> exists(Offer offer) {
        return offerRepository.existsById(offer.id());
    }
}