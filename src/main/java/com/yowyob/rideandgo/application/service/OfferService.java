package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.exception.OfferStatutNotMatchException;
import com.yowyob.rideandgo.domain.exception.UserIsNotDriverException;
import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.enums.OfferState;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.AcceptedOfferUseCase;
import com.yowyob.rideandgo.domain.ports.in.CreateOfferUseCase;
import com.yowyob.rideandgo.domain.ports.in.ResponseToOfferUseCase;
import com.yowyob.rideandgo.domain.ports.out.*;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.NotificationType;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService implements CreateOfferUseCase, ResponseToOfferUseCase, AcceptedOfferUseCase {
    private final OfferCachePort cache;
    private final OfferRepositoryPort repository;
    private final UserRepositoryPort userRepositoryPort;
    private final SendNotificationPort sendNotificationPort;
    private final RideRepositoryPort rideRepositoryPort;

    @Value("${application.kafka.notification-service.template.new-offer-id}")
    private int templateCreateOfferId;

    /**
     * UC-01: Creates a new offer and notifies all available drivers via Kafka/Email.
     */
    @Override
    public Mono<Offer> createOffer(Offer request, UUID passengerId) {
        Offer offer = Offer.builder()
                .id(Utils.generateUUID())
                .passengerId(passengerId)
                .startPoint(request.startPoint())
                .endPoint(request.endPoint())
                .price(request.price())
                .state(OfferState.PENDING) // Initial state according to spec
                .interestedDrivers(new ArrayList<>())
                .build();

        return repository.save(offer)
                .flatMap(savedOffer -> 
                    // Fetch all driver emails from the central database
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

                            // Parallel: Notify and Cache
                            return Mono.zip(
                                sendNotificationPort.sendNotification(notification),
                                cache.saveInCache(savedOffer)
                            ).thenReturn(savedOffer);
                        })
                )
                .doOnSuccess(o -> log.info("Offer successfully created: {}", o.id()));
    }

    /**
     * UC-02 Part 1: Allows a driver to apply for an offer (Manifest interest).
     */
    @Override
    public Mono<Offer> responseToOffer(UUID offerId, UUID driverId) {
        log.info("Driver {} applying for offer {}", driverId, offerId);
        
        return repository.findById(offerId)
                .flatMap(offer -> userRepositoryPort.findUserById(driverId)
                        .flatMap(user -> {
                            // Check if the user has the DRIVER role in the new Set-based model
                            boolean isDriver = user.roles().stream()
                                    .anyMatch(role -> role.type() == RoleType.DRIVER);

                            if (!isDriver) {
                                return Mono.error(new UserIsNotDriverException("User " + driverId + " is not authorized to drive."));
                            }

                            List<UUID> driverIds = new ArrayList<>(offer.interestedDrivers());
                            if (!driverIds.contains(driverId)) {
                                driverIds.add(driverId);
                            }

                            Offer updated = offer.withInterestedDriversAndState(driverIds, OfferState.BID_RECEIVED);
                            
                            return repository.save(updated)
                                    .flatMap(s -> cache.saveInCache(s).thenReturn(s));
                        })
                );
    }

    /**
     * UC-02 Part 2: Passenger selects a driver, offer is validated and becomes a Ride.
     */
    @Override
    public Mono<Ride> acceptedOffer(UUID offerId, UUID passengerId, UUID driverId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    // Business rules validation
                    if (offer.state() != OfferState.BID_RECEIVED && offer.state() != OfferState.DRIVER_SELECTED) {
                        return Mono.error(new OfferStatutNotMatchException("Offer must be in BID_RECEIVED state to be validated."));
                    }
                    if (!offer.interestedDrivers().contains(driverId)) {
                        return Mono.error(new IllegalArgumentException("Driver has not applied to this offer."));
                    }

                    // Move offer to final state
                    Offer validatedOffer = offer.withState(OfferState.VALIDATED);
                    return repository.save(validatedOffer);
                })
                .flatMap(offer -> {
                    // Create the Trip context (Section 6 of spec.md)
                    Ride ride = Ride.builder()
                            .id(Utils.generateUUID())
                            .offerId(offer.id())
                            .passengerId(passengerId)
                            .driverId(driverId)
                            .state(RideState.CREATED) // Initial trip state
                            .build();

                    return rideRepositoryPort.save(ride);
                })
                .doOnSuccess(r -> log.info("Trip successfully created for offer {}", offerId));
    }
}