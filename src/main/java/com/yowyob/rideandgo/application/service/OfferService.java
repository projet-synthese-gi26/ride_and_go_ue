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
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService implements CreateOfferUseCase, ResponseToOfferUseCase, AcceptedOfferUseCase {
    private final OfferCachePort cache;
    private final OfferRepositoryPort repository;
    private final OfferEventPublisherPort eventPublisher;
    private final UserRepositoryPort userRepositoryPort;
    private final SendNotificationPort sendNotificationPort;
    private final RideRepositoryPort rideRepositoryPort;

    @Value("${application.kafka.notification-service.template.new-offer-id}")
    private int templateCreateOfferId;

    @Override
    public Mono<Offer> createOffer(Offer request, UUID passengerId) {
        Offer offer = new Offer(
                Utils.generateUUID(),
                passengerId,
                request.startPoint(),
                request.endPoint(),
                request.price(),
                OfferState.NEW,
                new ArrayList<>(),
                null
        );

        Map<String, Object> data = new HashMap<>();
        data.put("startPoint", offer.startPoint());
        data.put("endPoint", offer.endPoint());
        data.put("price", offer.price());

        return repository.save(offer)
                .flatMap(savedOffer -> 
                    userRepositoryPort.findByRoleName(RoleType.DRIVER)
                        .map(User::email)
                        .collectList()
                        .flatMap(emails -> {
                            SendNotificationRequest notification = SendNotificationRequest.builder()
                                    .notificationType(NotificationType.EMAIL)
                                    .templateId(templateCreateOfferId)
                                    .to(emails)
                                    .data(data)
                                    .build();
                            
                            return Mono.zip(
                                sendNotificationPort.sendNotification(notification),
                                cache.saveInCache(savedOffer)
                            ).thenReturn(savedOffer);
                        })
                )
                .doOnSuccess(p -> log.info("Offer successfully created and drivers notified: {}", p.id()));
    }

    @Override
    public Mono<Offer> responseToOffer(UUID offerId, UUID driverId) {
        log.info("Response to offer {} by driver {}", offerId, driverId);
        return cache.findOfferById(offerId)
                .switchIfEmpty(repository.findById(offerId))
                .flatMap(offer -> userRepositoryPort.findUserById(driverId)
                        .flatMap(driver -> {
                            boolean isDriver = driver.roles().stream()
                                    .anyMatch(r -> r.type().equals(RoleType.DRIVER));

                            if (!isDriver) {
                                log.error("User {} is not driver", driverId);
                                return Mono.error(new UserIsNotDriverException("User " + driverId + " is not driver."));
                            }

                            List<UUID> interestedDrivers = new ArrayList<>(offer.interestedDrivers());
                            interestedDrivers.add(driverId);

                            Offer updatedOffer = offer.withInterestedDriversAndState(interestedDrivers, OfferState.CHOSEN);

                            return repository.save(updatedOffer)
                                    .flatMap(saved -> cache.saveInCache(saved).thenReturn(saved));
                        })
                );
    }

    @Override
    public Mono<Ride> acceptedOffer(UUID offerId, UUID passengerId, UUID driverId) {
        return cache.findOfferById(offerId)
                .switchIfEmpty(repository.findById(offerId))
                .flatMap(offer -> {
                    if (!offer.state().equals(OfferState.CHOSEN) && !offer.state().equals(OfferState.PENDING)) {
                        return Mono.error(new OfferStatutNotMatchException("Offer " + offerId + " is not in valid state."));
                    }

                    if (!offer.interestedDrivers().contains(driverId)) {
                        return Mono.error(new IllegalArgumentException("Driver not in interested list."));
                    }

                    if (!offer.passengerId().equals(passengerId)) {
                        return Mono.error(new IllegalArgumentException("Passenger mismatch."));
                    }

                    Offer terminatedOffer = offer.withState(OfferState.TERMINATED);
                    return repository.save(terminatedOffer);
                })
                .flatMap(savedOffer -> {
                    Ride ride = Ride.builder()
                            .id(Utils.generateUUID())
                            .offerId(savedOffer.id())
                            .passengerId(passengerId)
                            .driverId(driverId)
                            .state(RideState.CONFIRMED)
                            .timeReal(0)
                            .build();

                    return rideRepositoryPort.save(ride);
                });
    }
}