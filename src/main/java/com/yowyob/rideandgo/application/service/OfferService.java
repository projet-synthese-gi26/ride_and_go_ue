package com.yowyob.rideandgo.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.exception.OfferNotFoundException;
import com.yowyob.rideandgo.domain.exception.OfferStatutNotMatchException;
import com.yowyob.rideandgo.domain.model.*;
import com.yowyob.rideandgo.domain.model.enums.OfferState;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.domain.ports.in.*;
import com.yowyob.rideandgo.domain.ports.out.*;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.NotificationType;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService implements
        CreateOfferUseCase,
        ResponseToOfferUseCase,
        GetAvailableOffersUseCase,
        SelectDriverUseCase,
        OfferManagementUseCase {

    private final OfferRepositoryPort repository;
    private final UserRepositoryPort userRepositoryPort;
    private final SendNotificationPort sendNotificationPort;
    private final RideRepositoryPort rideRepositoryPort;
    private final LocationCachePort locationCachePort;
    private final EtaCalculatorService etaCalculatorService;
    private final TrackingCalculatorService trackingCalculatorService;
    private final DriverRepositoryPort driverRepositoryPort;
    private final VehicleRepositoryPort vehicleRepositoryPort;
    private final PaymentPort paymentPort;
    private final NotificationSettingsRepositoryPort settingsRepositoryPort;
    private final NotificationHistoryRepositoryPort historyRepositoryPort;
    private final UserDeviceRepositoryPort userDeviceRepositoryPort;
    private final ExternalUserPort externalUserPort;
    private final OfferCachePort cache;
    private final ObjectMapper objectMapper;

    @Value("${application.payment.commission-rate:0.10}")
    private double commissionRate;

    @Value("${application.notification.templates.new-offer:1}")
    private int tmplNewOffer;
    @Value("${application.notification.templates.driver-applied:2}")
    private int tmplDriverApplied;
    @Value("${application.notification.templates.driver-selected:3}")
    private int tmplDriverSelected;
    @Value("${application.notification.templates.ride-confirmed:4}")
    private int tmplRideConfirmed;
    @Value("${application.notification.templates.ride-cancelled:5}")
    private int tmplRideCancelled;

    // ==================================================================================
    // 1. CRÉATION D'OFFRE (PASSAGER)
    // ==================================================================================
    @Override
    public Mono<Offer> createOffer(Offer request, UUID callerId) {
        return userRepositoryPort.findUserById(callerId)
                .flatMap(user -> {
                    // Si le front n'a pas envoyé de numéro spécifique, on prend celui de l'user
                    String finalPhone = (request.passengerPhone() != null && !request.passengerPhone().isBlank()) 
                                        ? request.passengerPhone() 
                                        : user.telephone();

                    Offer offer = Offer.builder()
                            .id(Utils.generateUUID())
                            .passengerId(callerId)
                            .startPoint(request.startPoint())
                            .endPoint(request.endPoint())
                            .price(request.price())
                            .passengerPhone(finalPhone) // ✅
                            .departureTime(request.departureTime()) // ✅
                            .state(OfferState.PENDING)
                            .bids(new ArrayList<>())
                            .build();

                    return repository.save(offer)
                            .flatMap(saved -> cache.saveInCache(saved).thenReturn(saved));
                });
    }

    private Mono<User> ensureUserExistsLocally(UUID userId) {
        return userRepositoryPort.findUserById(userId)
                .switchIfEmpty(Mono.defer(() -> externalUserPort.fetchRemoteUserById(userId)
                        .flatMap(userRepositoryPort::save)));
    }

    // ==================================================================================
    // 2. DISCOVERY (CHAUFFEUR)
    // ==================================================================================
    @Override
    public Flux<Offer> getAvailableOffers() {
        return repository.findAll()
                .filter(o -> o.state() == OfferState.PENDING || o.state() == OfferState.BID_RECEIVED);
    }

    // ==================================================================================
    // 3. CANDIDATURE (CHAUFFEUR)
    // ==================================================================================
    @Override
    public Mono<Offer> responseToOffer(UUID offerId, UUID driverId) {
        return ensureUserExistsLocally(driverId)
                .flatMap(driver -> repository.findById(offerId)
                        .flatMap(offer -> paymentPort.getWalletByOwnerId(driverId)
                                .flatMap(wallet -> {
                                    // VÉRIFICATION : prix de l'offre * commission-rate < balance
                                    double estimatedCommission = offer.price() * commissionRate;
                                    if (wallet.balance() < estimatedCommission) {
                                        return Mono.error(new IllegalStateException(
                                                "Insufficient wallet balance to cover commission. Balance: "
                                                        + wallet.balance()));
                                    }
                                    return Mono.just(offer);
                                }))
                        .flatMap(offer -> {
                            if (offer.hasDriverApplied(driverId))
                                return Mono.just(offer);

                            List<Bid> currentBids = offer.bids() != null ? new ArrayList<>(offer.bids())
                                    : new ArrayList<>();
                            currentBids.add(Bid.builder().driverId(driverId).build());

                            log.info("🚀 Driver {} applying to Offer {}.", driverId, offerId);

                            return repository.save(offer.withBids(currentBids).withState(OfferState.BID_RECEIVED))
                                    .flatMap(s -> cache.saveInCache(s).thenReturn(s))
                                    .flatMap(saved -> {
                                        saveAndDispatch(
                                                saved.passengerId(),
                                                tmplDriverApplied,
                                                "Nouvelle candidature",
                                                driver.name() + " a postulé pour votre course.",
                                                Map.of("driverName", driver.name(), "offerId", offerId.toString()))
                                                .subscribe();
                                        return Mono.just(saved);
                                    });
                        }));
    }

    // ==================================================================================
    // 4. SÉLECTION (PASSAGER)
    // ==================================================================================
    @Override
    public Mono<Offer> selectDriver(UUID offerId, UUID driverId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (!offer.hasDriverApplied(driverId)) {
                        return Mono.error(new IllegalArgumentException("Driver has not applied."));
                    }
                    Offer updated = offer.withDriverSelected(driverId);
                    return repository.save(updated)
                            .flatMap(saved -> cache.saveInCache(saved).thenReturn(saved))
                            .flatMap(saved -> {
                                saveAndDispatch(
                                        driverId,
                                        tmplDriverSelected,
                                        "Course obtenue !",
                                        "Le client vous a sélectionné pour la course.",
                                        Map.of("offerId", offerId.toString(), "price", String.valueOf(offer.price())))
                                        .subscribe();
                                return Mono.just(saved);
                            });
                });
    }

    // ==================================================================================
    // 5. CONFIRMATION & CRÉATION TRIP (CHAUFFEUR)
    // ==================================================================================
    @Transactional
    public Mono<Ride> driverAcceptsOffer(UUID offerId, UUID driverId) {
        log.info("🏁 Driver {} is accepting Offer {}", driverId, offerId);

        return repository.findById(offerId)
                .switchIfEmpty(Mono.error(new OfferNotFoundException("Offre introuvable")))
                .flatMap(offer -> {
                    // 1. Vérifications de sécurité
                    if (offer.state() != OfferState.DRIVER_SELECTED) {
                        return Mono.error(new OfferStatutNotMatchException("L'offre n'est pas en attente de confirmation."));
                    }
                    if (offer.selectedDriverId() == null || !offer.selectedDriverId().equals(driverId)) {
                        return Mono.error(new IllegalStateException("Vous n'êtes pas le chauffeur sélectionné pour cette offre."));
                    }

                    // 2. Paiement de la commission et Mise à jour de l'Offre
                    return paymentPort.getWalletByOwnerId(driverId)
                            .flatMap(wallet -> paymentPort.processPayment(wallet.id(), offer.price()))
                            .then(repository.save(offer.withState(OfferState.VALIDATED))) // ✅ L'OFFRE PASSE EN VALIDATED
                            .doOnSuccess(savedOffer -> log.info("✅ Offer {} state updated to VALIDATED", offerId))
                            .thenReturn(offer);
                })
                .flatMap(offer -> {
                    // 3. Création de la Course (Ride)
                    Ride ride = Ride.builder()
                            .id(Utils.generateUUID())
                            .offerId(offer.id())
                            .passengerId(offer.passengerId())
                            .driverId(driverId)
                            .distance(0.0) // Sera mis à jour pendant la course
                            .duration(0)
                            .state(RideState.CREATED) // ✅ LA COURSE DÉBUTE EN 'CREATED'
                            .build();

                    return rideRepositoryPort.save(ride)
                            .flatMap(savedRide -> {
                                log.info("🚀 Ride created: {}", savedRide.id());
                                // Notification asynchrone
                                saveAndDispatch(
                                        offer.passengerId(),
                                        tmplRideConfirmed,
                                        "Chauffeur en route",
                                        "Votre chauffeur a confirmé et arrive.",
                                        Map.of("rideId", savedRide.id().toString(), "driverId", driverId.toString()))
                                        .subscribe();
                                return Mono.just(savedRide);
                            });
                });
    }

    // ==================================================================================
    // 6. ANNULATION (PASSAGER)
    // ==================================================================================
    public Mono<Offer> cancelOffer(UUID offerId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.state() == OfferState.VALIDATED) {
                        return Mono.error(new IllegalStateException("Cannot cancel validated offer."));
                    }
                    Mono<Offer> savedOperation = repository.save(offer.withState(OfferState.CANCELLED));
                    if (offer.selectedDriverId() != null) {
                        saveAndDispatch(
                                offer.selectedDriverId(),
                                tmplRideCancelled,
                                "Course annulée",
                                "Le client a annulé la course.",
                                Map.of("offerId", offerId.toString())).subscribe();
                    }
                    return savedOperation;
                });
    }

    // ==================================================================================
    // CENTRALISATION : NOTIFICATIONS ET HISTORIQUE
    // ==================================================================================

    /**
     * NOTIFICATION FILTRÉE : Chauffeurs en ligne, complets, validés ET avec solde
     * suffisant.
     */
    private Mono<Void> notifyEligibleDriversWithBalance(Offer offer) {
        log.info("📢 Notifying eligible drivers for offer {} (Price: {})", offer.id(), offer.price());

        double requiredBalance = offer.price() * commissionRate;

        return driverRepositoryPort.findAll()
                .filter(d -> d.isOnline() && d.isProfileCompleted() && d.isProfileValidated())
                .flatMap(driver -> paymentPort.getWalletByOwnerId(driver.id())
                        .filter(wallet -> wallet.balance() >= requiredBalance)
                        .flatMap(wallet -> userRepositoryPort.findUserById(driver.id()))
                        .map(User::email))
                .collectList()
                .flatMap(emails -> {
                    if (emails.isEmpty())
                        return Mono.empty();

                    Map<String, String> data = Map.of(
                            "offerId", offer.id().toString(),
                            "price", String.valueOf(offer.price()),
                            "start", offer.startPoint(),
                            "end", offer.endPoint());

                    return send(NotificationType.EMAIL, tmplNewOffer, emails, data);
                })
                .then();
    }

    private Mono<Void> saveAndDispatch(UUID userId, int templateId, String title, String message,
            Map<String, String> data) {
        String json = "{}";
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("Serialization failed", e);
        }

        Notification history = Notification.builder()
                .id(Utils.generateUUID())
                .userId(userId)
                .title(title)
                .message(message)
                .type("INFO")
                .isRead(false)
                .dataJson(json)
                .build();

        return historyRepositoryPort.save(history).then(dispatchNotification(userId, templateId, data));
    }

    private Mono<Void> dispatchNotification(UUID userId, int templateId, Map<String, String> data) {
        return settingsRepositoryPort.getSettings(userId)
                .flatMap(settings -> {
                    List<Mono<Boolean>> sendingTasks = new ArrayList<>();
                    if (settings.pushEnabled()) {
                        sendingTasks.add(userDeviceRepositoryPort.findDeviceTokenByUserId(userId)
                                .flatMap(token -> send(NotificationType.PUSH, templateId, List.of(token), data))
                                .defaultIfEmpty(false));
                    }
                    if (settings.emailEnabled()) {
                        sendingTasks.add(userRepositoryPort.findUserById(userId)
                                .flatMap(
                                        user -> send(NotificationType.EMAIL, templateId, List.of(user.email()), data)));
                    }
                    return Flux.merge(sendingTasks).then();
                });
    }

    private Mono<Boolean> send(NotificationType type, int tmpl, List<String> to, Map<String, String> data) {
        return sendNotificationPort.sendNotification(
                SendNotificationRequest.builder()
                        .notificationType(type)
                        .templateId(tmpl)
                        .to(to)
                        .data(new HashMap<>(data))
                        .build());
    }

    // ==================================================================================
    // ENRICHISSEMENT (UI) AVEC DONNÉES RÉELLES
    // ==================================================================================

    public Mono<Offer> getOfferWithEnrichedBids(UUID offerId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.bids() == null || offer.bids().isEmpty())
                        return Mono.just(offer);

                    // 1. Récupérer la position du passager (point de départ) pour calculs
                    return locationCachePort.getLocation(offer.passengerId())
                            .defaultIfEmpty(new LocationCachePort.Location(0.0, 0.0))
                            .flatMap(pLoc -> Flux.fromIterable(offer.bids())
                                    .flatMap(bid -> enrichSingleBid(bid, pLoc))
                                    .collectList()
                                    .map(offer::withBids));
                });
    }

    /**
     * Orchestre la récupération des données réelles pour un seul chauffeur.
     */
    private Mono<Bid> enrichSingleBid(Bid bid, LocationCachePort.Location pLoc) {
        UUID dId = bid.driverId();
        return Mono.zip(
                userRepositoryPort.findUserById(dId),
                driverRepositoryPort.findById(dId),
                locationCachePort.getLocation(dId).defaultIfEmpty(new LocationCachePort.Location(0.0, 0.0))
        ).flatMap(tuple -> {
            User user = tuple.getT1();
            Driver driver = tuple.getT2();
            LocationCachePort.Location dLoc = tuple.getT3();

            return Mono.justOrEmpty(driver.vehicleId())
                    .flatMap(vehicleRepositoryPort::getVehicleById)
                    .defaultIfEmpty(Vehicle.builder().brand("N/A").build())
                    .map(v -> Bid.builder()
                            .driverId(dId)
                            .driverName(user.firstName() + " " + user.lastName())
                            .driverPhone(user.telephone()) // ✅ AJOUT DU NUMÉRO
                            .driverPhoto(user.photoUri())
                            .latitude(dLoc.latitude())
                            .longitude(dLoc.longitude())
                            .brand(v.brand())
                            .model(v.vehicleModelId())
                            .licensePlate(v.registrationNumber())
                            .build());
        });
    }
    
    // ==================================================================================
    // CRUD ET GESTION
    // ==================================================================================

    @Override
    public Flux<Offer> getAllOffers() {
        return repository.findAll();
    }

    @Override
    public Mono<Offer> getOfferById(UUID id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new OfferNotFoundException("Offer not found: " + id)));
    }

@Override
    public Mono<Offer> updateOffer(UUID id, Offer offerDetails) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new OfferNotFoundException("Offer not found: " + id)))
                .flatMap(existingOffer -> {
                    // Construction de l'offre mise à jour avec les nouveaux champs
                    Offer updated = new Offer(
                            existingOffer.id(),
                            existingOffer.passengerId(),
                            existingOffer.selectedDriverId(),
                            offerDetails.startPoint() != null ? offerDetails.startPoint() : existingOffer.startPoint(),
                            offerDetails.endPoint() != null ? offerDetails.endPoint() : existingOffer.endPoint(),
                            offerDetails.price() > 0 ? offerDetails.price() : existingOffer.price(),
                            
                            // Gestion des nouveaux champs (Phone & Time)
                            (offerDetails.passengerPhone() != null && !offerDetails.passengerPhone().isBlank()) 
                                ? offerDetails.passengerPhone() : existingOffer.passengerPhone(),
                                
                            (offerDetails.departureTime() != null && !offerDetails.departureTime().isBlank()) 
                                ? offerDetails.departureTime() : existingOffer.departureTime(),
                                
                            existingOffer.state(),
                            existingOffer.bids(),
                            existingOffer.version()
                    );
                    
                    log.info("📝 Updating Offer {}: phone={}, time={}", id, updated.passengerPhone(), updated.departureTime());
                    
                    return repository.save(updated);
                })
                .flatMap(saved -> cache.saveInCache(saved).thenReturn(saved));
    }

    @Override
    public Mono<Boolean> deleteOffer(UUID id) {
        return repository.findById(id)
                .flatMap(offer -> repository.delete(offer).thenReturn(true))
                .defaultIfEmpty(false);
    }
}