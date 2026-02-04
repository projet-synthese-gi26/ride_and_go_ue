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
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.LandingOfferResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.NotificationType;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
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

    @Value("${application.offer.search-radius-km:20.0}")
    private double searchRadius;

    // ==================================================================================
    // 1. CRÉATION D'OFFRE (PASSAGER)
    // ==================================================================================
    @Override
    public Mono<Offer> createOffer(Offer request, UUID callerId) {
        return userRepositoryPort.findUserById(callerId)
                .flatMap(user -> {
                    // Logique métier : Si pas de numéro tiers, on prend celui du compte
                    String finalPhone = (request.passengerPhone() != null && !request.passengerPhone().isBlank())
                            ? request.passengerPhone()
                            : user.telephone();

                    Offer offer = Offer.builder()
                            .id(Utils.generateUUID())
                            .passengerId(callerId)
                            .startPoint(request.startPoint())
                            .startLat(request.startLat())
                            .startLon(request.startLon())
                            .endPoint(request.endPoint())
                            .price(request.price())
                            .passengerPhone(finalPhone)
                            .departureTime(request.departureTime())
                            .state(OfferState.PENDING)
                            .bids(new ArrayList<>())
                            .build();

                    // On utilise updateOfferState pour la cohérence DB/Redis Geo
                    return updateOfferState(offer, OfferState.PENDING)
                            .flatMap(saved -> {
                                // Lancement asynchrone du matching
                                this.notifyNearbyDrivers(saved).subscribe();
                                return Mono.just(saved);
                            });
                });
    }

    private Mono<Offer> updateOfferState(Offer offer, OfferState newState) {
        Offer updated = offer.withState(newState);
        return repository.save(updated)
                .flatMap(saved -> {
                    // Nettoyage de l'index de recherche si l'offre n'est plus "ouverte"
                    if (newState != OfferState.PENDING && newState != OfferState.BID_RECEIVED) {
                        return locationCachePort.removeOfferLocation(saved.id()).thenReturn(saved);
                    }
                    // Indexation si l'offre est active
                    return locationCachePort.saveOfferLocation(saved.id(), saved.startLat(), saved.startLon())
                            .thenReturn(saved);
                })
                .flatMap(saved -> cache.saveInCache(saved).thenReturn(saved));
    }

    private Mono<Void> notifyNearbyDrivers(Offer offer) {
        log.info("🎯 Start matching for Offer {} in radius {}km", offer.id(), searchRadius);

        // 1. Trouver les chauffeurs éligibles (On récupère l'objet User complet)
        return locationCachePort.findNearbyDrivers(offer.startLat(), offer.startLon(), searchRadius)
                .flatMap(geoResult -> {
                    UUID driverId = geoResult.driverId();
                    return driverRepositoryPort.findById(driverId)
                            .filter(d -> d.isOnline() && d.isProfileValidated())
                            .flatMap(driver -> paymentPort.getWalletByOwnerId(driverId)
                                    .filter(wallet -> wallet.balance() >= (offer.price() * commissionRate))
                                    .flatMap(wallet -> userRepositoryPort.findUserById(driverId))); // On retourne le
                                                                                                    // User
                })
                .collectList()
                .flatMap(users -> {
                    if (users.isEmpty()) {
                        log.warn("⚠️ No eligible drivers found within {}km for offer {}", searchRadius, offer.id());
                        return Mono.empty();
                    }

                    log.info("📢 Notifying {} nearby drivers for offer {}", users.size(), offer.id());

                    Map<String, String> data = Map.of(
                            "offerId", offer.id().toString(),
                            "price", String.valueOf(offer.price()),
                            "start", offer.startPoint());

                    // 2. Préparer la liste des emails pour l'envoi groupé
                    List<String> emails = new ArrayList<>();

                    // 3. Préparer les sauvegardes en base (Historique)
                    List<Mono<Void>> historySaves = new ArrayList<>();

                    String json = "{}";
                    try {
                        json = objectMapper.writeValueAsString(data);
                    } catch (Exception e) {
                    }

                    for (User user : users) {
                        emails.add(user.email());

                        // Création de l'entrée historique pour CHAQUE chauffeur
                        Notification history = Notification.builder()
                                .id(Utils.generateUUID())
                                .userId(user.id())
                                .title("Nouvelle course disponible")
                                .message("Une course de " + offer.price() + " F est disponible à " + offer.startPoint())
                                .type("OFFER")
                                .isRead(false)
                                .dataJson(json)
                                .build();

                        historySaves.add(historyRepositoryPort.save(history));
                    }

                    // 4. Exécuter : Sauvegarde Historique (Parallèle) ET Envoi Push (Groupé)
                    Mono<Boolean> sendTask = sendNotificationPort.sendNotification(
                            SendNotificationRequest.builder()
                                    .notificationType(NotificationType.EMAIL)
                                    .templateId(tmplNewOffer)
                                    .to(emails)
                                    .data(data)
                                    .build());

                    // On lance tout en même temps
                    return Flux.merge(historySaves).then(sendTask).then();
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
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> UUID.fromString(ctx.getAuthentication().getName()))
                .flatMapMany(driverId ->
                // TENTATIVE 1 : Recherche Géo
                locationCachePort.getLocation(driverId)
                        .flatMapMany(loc -> {
                            log.info("📍 Driver {} location found, searching within {}km", driverId, searchRadius);
                            return locationCachePort.findNearbyOfferIds(loc.latitude(), loc.longitude(), searchRadius)
                                    .flatMap(repository::findById);
                        })
                        // TENTATIVE 2 : Fallback si pas de position ou pas d'offres proches
                        .switchIfEmpty(Flux.defer(() -> {
                            log.warn("📡 No location for driver {} or no nearby offers. Falling back to latest offers.",
                                    driverId);
                            return repository.findLatestPending(20);
                        }))
                        // FILTRAGE & ENRICHISSEMENT (Commun aux deux cas)
                        .filter(o -> o.state() == OfferState.PENDING || o.state() == OfferState.BID_RECEIVED)
                        .distinct(Offer::id) // Évite les doublons si une offre est dans les deux flux
                        .flatMap(this::enrichOffer));
    }

public Flux<LandingOfferResponse> getLatestPublicOffers(int limit) {
    return repository.findLatestPending(limit)
            .map(offer -> new LandingOfferResponse(
                    offer.startPoint(),
                    offer.endPoint(),
                    offer.startLat(),
                    offer.startLon(),
                    offer.price(),
                    offer.departureTime(),
                    // On récupère la date de création depuis le domaine
                    // Note: Assure-toi que ton OfferMapper mappe bien createdDate vers createdAt
                    LocalDateTime.now() // LocalDateTime.now() // Par défaut si null, mais utilise offer.createdAt()
            ));
}

    // ==================================================================================
    // 3. CANDIDATURE (CHAUFFEUR)
    // ==================================================================================
    @Override
    public Mono<Offer> responseToOffer(UUID offerId, UUID driverId) {
        return ensureUserExistsLocally(driverId)
                .flatMap(driverUser -> repository.findById(offerId)
                        .switchIfEmpty(Mono.error(new OfferNotFoundException("Offre introuvable")))
                        .flatMap(offer -> paymentPort.getWalletByOwnerId(driverId)
                                .flatMap(wallet -> {
                                    // VÉRIFICATION COMMISSION : prix * taux < solde
                                    double estimatedCommission = offer.price() * commissionRate;
                                    if (wallet.balance() < estimatedCommission) {
                                        return Mono.error(new IllegalStateException(
                                                "Solde insuffisant pour couvrir la commission (" + estimatedCommission
                                                        + " F)."));
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

                            // Mise à jour de l'état vers BID_RECEIVED
                            return updateOfferState(offer.withBids(currentBids), OfferState.BID_RECEIVED)
                                    .flatMap(saved -> {
                                        saveAndDispatch(
                                                saved.passengerId(),
                                                tmplDriverApplied,
                                                "Nouvelle candidature",
                                                driverUser.name() + " a postulé pour votre course.",
                                                Map.of("driverName", driverUser.name(), "offerId", offerId.toString()))
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
                    // Sécurités
                    if (offer.state() != OfferState.DRIVER_SELECTED) {
                        return Mono.error(new OfferStatutNotMatchException("L'offre n'est plus disponible."));
                    }
                    if (offer.selectedDriverId() == null || !offer.selectedDriverId().equals(driverId)) {
                        return Mono.error(new IllegalStateException("Vous n'êtes pas le chauffeur sélectionné."));
                    }

                    // 1. Paiement effectif de la commission
                    return paymentPort.getWalletByOwnerId(driverId)
                            .flatMap(wallet -> paymentPort.processPayment(wallet.id(), offer.price()))
                            // 2. Mise à jour de l'Offre vers VALIDATED (Retrait de Redis Geo automatique)
                            .then(updateOfferState(offer, OfferState.VALIDATED))
                            .thenReturn(offer);
                })
                .flatMap(offer -> {
                    // 3. Création de la Course (Ride)
                    Ride ride = Ride.builder()
                            .id(Utils.generateUUID())
                            .offerId(offer.id())
                            .passengerId(offer.passengerId())
                            .driverId(driverId)
                            .distance(0.0)
                            .duration(0)
                            .state(RideState.CREATED)
                            .build();

                    return rideRepositoryPort.save(ride)
                            .flatMap(savedRide -> {
                                log.info("🚀 Ride created: {}", savedRide.id());
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
                .switchIfEmpty(Mono.error(new OfferNotFoundException("Offre introuvable")))
                .flatMap(offer -> {
                    if (offer.state() == OfferState.VALIDATED) {
                        return Mono.error(new IllegalStateException("Impossible d'annuler une offre déjà validée."));
                    }

                    return updateOfferState(offer, OfferState.CANCELLED)
                            .flatMap(saved -> {
                                if (offer.selectedDriverId() != null) {
                                    saveAndDispatch(
                                            offer.selectedDriverId(),
                                            tmplRideCancelled,
                                            "Course annulée",
                                            "Le client a annulé la course.",
                                            Map.of("offerId", offerId.toString())).subscribe();
                                }
                                return Mono.just(saved);
                            });
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

    private Mono<Offer> enrichOffer(Offer offer) {
        if (offer.bids() == null || offer.bids().isEmpty()) {
            return Mono.just(offer);
        }

        LocationCachePort.Location offerStartLoc = new LocationCachePort.Location(
                offer.startLat() != null ? offer.startLat() : 0.0,
                offer.startLon() != null ? offer.startLon() : 0.0);

        return Flux.fromIterable(offer.bids())
                .flatMap(bid -> enrichSingleBid(bid, offerStartLoc))
                .collectList()
                .map(offer::withBids);
    }

    // ==================================================================================
    // ENRICHISSEMENT (UI) AVEC DONNÉES RÉELLES
    // ==================================================================================

    public Mono<Offer> getOfferWithEnrichedBids(UUID offerId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.bids() == null || offer.bids().isEmpty())
                        return Mono.just(offer);

                    // CORRECTION ICI AUSSI
                    LocationCachePort.Location offerStartLoc = new LocationCachePort.Location(
                            offer.startLat() != null ? offer.startLat() : 0.0,
                            offer.startLon() != null ? offer.startLon() : 0.0);

                    return Flux.fromIterable(offer.bids())
                            .flatMap(bid -> enrichSingleBid(bid, offerStartLoc))
                            .collectList()
                            .map(offer::withBids);
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
                locationCachePort.getLocation(dId).defaultIfEmpty(new LocationCachePort.Location(0.0, 0.0)))
                .flatMap(tuple -> {
                    User user = tuple.getT1();
                    Driver driver = tuple.getT2();
                    LocationCachePort.Location dLoc = tuple.getT3();

                    double distance = 0.0;
                    int eta = 0;

                    // Calcul temps réel si les deux positions sont connues
                    if (pLoc.latitude() != 0.0 && dLoc.latitude() != 0.0) {
                        distance = trackingCalculatorService.calculateDistance(
                                pLoc.latitude(), pLoc.longitude(),
                                dLoc.latitude(), dLoc.longitude());
                        eta = trackingCalculatorService.calculateEtaInMinutes(distance);
                    }

                    final double finalDistance = distance;
                    final int finalEta = eta;

                    return Mono.justOrEmpty(driver.vehicleId())
                            .flatMap(vehicleRepositoryPort::getVehicleById)
                            .defaultIfEmpty(Vehicle.builder().brand("N/A").build())
                            .map(v -> Bid.builder()
                                    .driverId(dId)
                                    .driverName(user.firstName() + " " + user.lastName())
                                    .driverPhone(user.telephone())
                                    .driverPhoto(user.photoUri())
                                    .latitude(dLoc.latitude())
                                    .longitude(dLoc.longitude())
                                    .distanceToPassenger(finalDistance)
                                    .eta(finalEta)
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
        return repository.findAll()
                .flatMap(this::enrichOffer); // ✅ Enrichit toute la liste
    }

    @Override
    public Mono<Offer> getOfferById(UUID id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new OfferNotFoundException("Offer not found: " + id)))
                .flatMap(this::enrichOffer); // ✅ Enrichit l'offre unique
    }

    @Override
    public Mono<Offer> updateOffer(UUID id, Offer offerDetails) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new OfferNotFoundException("Offer not found: " + id)))
                .flatMap(existingOffer -> {
                    // Construction de l'offre mise à jour
                    Offer updated = new Offer(
                            existingOffer.id(),
                            existingOffer.passengerId(),
                            existingOffer.selectedDriverId(),
                            // Update Point Nom
                            offerDetails.startPoint() != null ? offerDetails.startPoint() : existingOffer.startPoint(),
                            // ✅ Update Coordonnées Géo
                            offerDetails.startLat() != null ? offerDetails.startLat() : existingOffer.startLat(),
                            offerDetails.startLon() != null ? offerDetails.startLon() : existingOffer.startLon(),

                            offerDetails.endPoint() != null ? offerDetails.endPoint() : existingOffer.endPoint(),
                            offerDetails.price() > 0 ? offerDetails.price() : existingOffer.price(),

                            // Phone & Time
                            (offerDetails.passengerPhone() != null && !offerDetails.passengerPhone().isBlank())
                                    ? offerDetails.passengerPhone()
                                    : existingOffer.passengerPhone(),

                            (offerDetails.departureTime() != null && !offerDetails.departureTime().isBlank())
                                    ? offerDetails.departureTime()
                                    : existingOffer.departureTime(),

                            existingOffer.state(),
                            existingOffer.bids(),
                            existingOffer.version());

                    log.info("📝 Updating Offer {}: Lat={}, Lon={}", id, updated.startLat(), updated.startLon());

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