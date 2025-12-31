package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.exception.OfferStatutNotMatchException;
import com.yowyob.rideandgo.domain.exception.UserIsNotDriverException;
import com.yowyob.rideandgo.domain.model.Bid;
import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.enums.OfferState;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.*;
import com.yowyob.rideandgo.domain.ports.out.*;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.NotificationType;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService implements 
    CreateOfferUseCase, 
    ResponseToOfferUseCase, 
    GetAvailableOffersUseCase,
    SelectDriverUseCase {

    private final OfferCachePort cache;
    private final OfferRepositoryPort repository;
    private final UserRepositoryPort userRepositoryPort;
    private final SendNotificationPort sendNotificationPort;
    private final RideRepositoryPort rideRepositoryPort;
    private final LocationCachePort locationCachePort;
    private final EtaCalculatorService etaCalculatorService;

    @Value("${application.kafka.notification-service.template.new-offer-id}")
    private int templateCreateOfferId;

    // --- 1. PUBLICATION ---
    @Override
    public Mono<Offer> createOffer(Offer request, UUID passengerId) {
        Offer offer = Offer.builder()
                .id(Utils.generateUUID())
                .passengerId(passengerId)
                .startPoint(request.startPoint())
                .endPoint(request.endPoint())
                .price(request.price())
                .state(OfferState.PENDING)
                .bids(new ArrayList<>())
                .build();

        return repository.save(offer)
                .flatMap(saved -> cache.saveInCache(saved).thenReturn(saved))
                .doOnSuccess(o -> log.info("Offer created: {}", o.id()));
    }

    // --- 2. DECOUVERTE ---
    @Override
    public Flux<Offer> getAvailableOffers() {
        return repository.findAll()
                .filter(o -> o.state() == OfferState.PENDING || o.state() == OfferState.BID_RECEIVED);
    }

    // --- 3. CANDIDATURE ---
    @Override
    public Mono<Offer> responseToOffer(UUID offerId, UUID driverId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.hasDriverApplied(driverId)) return Mono.just(offer);
                    
                    List<Bid> currentBids = new ArrayList<>(offer.bids());
                    currentBids.add(Bid.builder().driverId(driverId).build());
                    
                    return repository.save(offer.withBids(currentBids).withState(OfferState.BID_RECEIVED))
                            .flatMap(s -> cache.saveInCache(s).thenReturn(s));
                });
    }

    // --- 4. SÉLECTION (PASSAGER) ---
    @Override
    public Mono<Offer> selectDriver(UUID offerId, UUID driverId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (!offer.hasDriverApplied(driverId)) {
                        return Mono.error(new IllegalArgumentException("Driver has not applied."));
                    }
                    // TODO: Idéalement, stocker driverId sélectionné dans l'offre pour sécuriser le accept ensuite
                    return repository.save(offer.withState(OfferState.DRIVER_SELECTED));
                });
    }

    // --- 5. CONFIRMATION (CHAUFFEUR) ---
    public Mono<Ride> driverAcceptsOffer(UUID offerId, UUID driverId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.state() != OfferState.DRIVER_SELECTED) {
                        return Mono.error(new OfferStatutNotMatchException("Offer must be in DRIVER_SELECTED state."));
                    }
                    // Passage à VALIDATED
                    return repository.save(offer.withState(OfferState.VALIDATED));
                })
                .flatMap(offer -> {
                    // Création de la course
                    Ride ride = Ride.builder()
                            .id(Utils.generateUUID())
                            .offerId(offer.id())
                            .passengerId(offer.passengerId())
                            .driverId(driverId)
                            .state(RideState.CREATED) // État initial: Chauffeur en route
                            .build();

                    return rideRepositoryPort.save(ride);
                });
    }

    // --- 6. ANNULATION (PASSAGER) ---
    public Mono<Offer> cancelOffer(UUID offerId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.state() == OfferState.VALIDATED) {
                        return Mono.error(new IllegalStateException("Cannot cancel a validated offer. Cancel the ride instead."));
                    }
                    return repository.save(offer.withState(OfferState.CANCELLED));
                });
    }

    // --- UTILITAIRE (Enrichissement) ---
    public Mono<Offer> getOfferWithEnrichedBids(UUID offerId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.bids().isEmpty()) return Mono.just(offer);
                    return Flux.fromIterable(offer.bids())
                            .flatMap(bid -> Mono.zip(
                                    userRepositoryPort.findUserById(bid.driverId()),
                                    locationCachePort.getLocation(bid.driverId()).defaultIfEmpty(new LocationCachePort.Location(0.0, 0.0))
                            ).flatMap(tuple -> etaCalculatorService.calculateEta(tuple.getT2().latitude(), tuple.getT2().longitude(), 0.0, 0.0)
                                    .map(eta -> Bid.builder()
                                            .driverId(tuple.getT1().id())
                                            .driverName(tuple.getT1().name())
                                            .latitude(tuple.getT2().latitude())
                                            .longitude(tuple.getT2().longitude())
                                            .eta(eta).rating(4.8).build())))
                            .collectList()
                            .map(offer::withBids);
                });
    }
}