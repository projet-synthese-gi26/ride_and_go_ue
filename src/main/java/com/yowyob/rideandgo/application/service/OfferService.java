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
    AcceptedOfferUseCase,
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

    /**
     * UC-01: Create offer and notify drivers via Kafka
     */
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
                .flatMap(savedOffer -> 
                    userRepositoryPort.findByRoleName(RoleType.DRIVER)
                        .map(User::email)
                        .collectList()
                        .flatMap(driverEmails -> {
                            SendNotificationRequest notification = SendNotificationRequest.builder()
                                    .notificationType(NotificationType.EMAIL)
                                    .templateId(templateCreateOfferId)
                                    .to(driverEmails)
                                    .data(Map.of("price", offer.price(), "from", offer.startPoint(), "to", offer.endPoint()))
                                    .build();

                            return Mono.zip(
                                sendNotificationPort.sendNotification(notification),
                                cache.saveInCache(savedOffer)
                            ).thenReturn(savedOffer);
                        })
                )
                .doOnSuccess(o -> log.info("Offer created and drivers notified: {}", o.id()));
    }

    /**
     * UC: Discovery - List offers for drivers
     */
    @Override
    public Flux<Offer> getAvailableOffers() {
        // En vrai on filtrerait par geo-zone. Ici on filtre par état.
        return repository.findAll()
                .filter(o -> o.state() == OfferState.PENDING || o.state() == OfferState.BID_RECEIVED);
    }

    /**
     * UC-02: Driver applies to an offer
     */
    @Override
    public Mono<Offer> responseToOffer(UUID offerId, UUID driverId) {
        return repository.findById(offerId)
                .flatMap(offer -> userRepositoryPort.findUserById(driverId)
                        .flatMap(user -> {
                            boolean isDriver = user.roles().stream()
                                    .anyMatch(role -> role.type() == RoleType.DRIVER);

                            if (!isDriver) {
                                return Mono.error(new UserIsNotDriverException("User is not a driver."));
                            }

                            if (offer.hasDriverApplied(driverId)) {
                                return Mono.just(offer);
                            }

                            List<Bid> currentBids = new ArrayList<>(offer.bids());
                            currentBids.add(Bid.builder().driverId(driverId).build());

                            Offer updated = offer.withBids(currentBids).withState(OfferState.BID_RECEIVED);
                            
                            return repository.save(updated)
                                    .flatMap(s -> cache.saveInCache(s).thenReturn(s));
                        })
                );
    }

    /**
     * SPECIAL: Dynamic Enrichment (Calculates GPS + ETA on the fly)
     */
    public Mono<Offer> getOfferWithEnrichedBids(UUID offerId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.bids().isEmpty()) return Mono.just(offer);

                    return Flux.fromIterable(offer.bids())
                            .flatMap(bid -> Mono.zip(
                                    userRepositoryPort.findUserById(bid.driverId()),
                                    locationCachePort.getLocation(bid.driverId())
                                        .defaultIfEmpty(new LocationCachePort.Location(0.0, 0.0))
                            ).flatMap(tuple -> {
                                User user = tuple.getT1();
                                LocationCachePort.Location loc = tuple.getT2();
                                
                                return etaCalculatorService.calculateEta(loc.latitude(), loc.longitude(), 0.0, 0.0)
                                        .map(eta -> Bid.builder()
                                                .driverId(user.id())
                                                .driverName(user.name())
                                                .rating(4.8) // Mock rating
                                                .latitude(loc.latitude())
                                                .longitude(loc.longitude())
                                                .eta(eta)
                                                .build());
                            }))
                            .collectList()
                            .map(offer::withBids);
                });
    }

    /**
     * UC: Passenger selects a specific driver
     */
    @Override
    public Mono<Offer> selectDriver(UUID offerId, UUID driverId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (!offer.hasDriverApplied(driverId)) {
                        return Mono.error(new IllegalArgumentException("Driver has not applied."));
                    }
                    return repository.save(offer.withState(OfferState.DRIVER_SELECTED));
                });
    }

    /**
     * UC-03: Offer validation and Ride creation
     */
    @Override
    public Mono<Ride> acceptedOffer(UUID offerId, UUID passengerId, UUID driverId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.state() != OfferState.BID_RECEIVED && offer.state() != OfferState.DRIVER_SELECTED) {
                        return Mono.error(new OfferStatutNotMatchException("Offer state doesn't allow validation."));
                    }
                    return repository.save(offer.withState(OfferState.VALIDATED));
                })
                .flatMap(offer -> {
                    Ride ride = Ride.builder()
                            .id(Utils.generateUUID())
                            .offerId(offer.id())
                            .passengerId(passengerId)
                            .driverId(driverId)
                            .state(RideState.CREATED)
                            .build();

                    return rideRepositoryPort.save(ride);
                })
                .doOnSuccess(r -> log.info("Ride context created: {}", r.id()));
    }
}