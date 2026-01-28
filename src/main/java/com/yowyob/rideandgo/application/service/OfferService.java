package com.yowyob.rideandgo.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.exception.OfferNotFoundException;
import com.yowyob.rideandgo.domain.exception.OfferStatutNotMatchException;
import com.yowyob.rideandgo.domain.model.Bid;
import com.yowyob.rideandgo.domain.model.Notification;
import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.enums.OfferState;
import com.yowyob.rideandgo.domain.model.enums.RideState;
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
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService implements
        CreateOfferUseCase,
        ResponseToOfferUseCase,
        GetAvailableOffersUseCase,
        SelectDriverUseCase,
        OfferManagementUseCase {

    private final OfferCachePort cache;
    private final OfferRepositoryPort repository;
    private final UserRepositoryPort userRepositoryPort;
    private final SendNotificationPort sendNotificationPort;
    private final RideRepositoryPort rideRepositoryPort;
    private final LocationCachePort locationCachePort;
    private final EtaCalculatorService etaCalculatorService;

    // Nouveaux ports pour la gestion avancée des notifications
    private final DriverRepositoryPort driverRepositoryPort;
    private final UserDeviceRepositoryPort userDeviceRepositoryPort;
    private final NotificationSettingsRepositoryPort settingsRepositoryPort;
    private final NotificationHistoryRepositoryPort historyRepositoryPort;
    private final ObjectMapper objectMapper;

    private final ExternalUserPort externalUserPort;

    // IDs des Templates (Injectés depuis application.yml)
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

    // Données de démo pour l'UI (Simulation Véhicules)
    private record DemoVehicle(String model, String color, String plate, String image) {
    }

    private static final List<DemoVehicle> FAKE_FLEET = List.of(
            new DemoVehicle("Toyota Yaris", "Grise", "LT-882-AF", "https://randomuser.me/api/portraits/men/32.jpg"),
            new DemoVehicle("Hyundai Elantra", "Blanche", "CE-102-ZZ",
                    "https://randomuser.me/api/portraits/men/45.jpg"),
            new DemoVehicle("Peugeot 301", "Noire", "LT-440-GG", "https://randomuser.me/api/portraits/men/22.jpg"));

    // ==================================================================================
    // 1. CRÉATION D'OFFRE (PASSAGER)
    // ==================================================================================
    @Override
    public Mono<Offer> createOffer(Offer request, UUID passengerId) {
        // ÉTAPE CRITIQUE : S'assurer que le passager existe localement avant de créer
        // l'offre
        return ensureUserExistsLocally(passengerId)
                .flatMap(user -> {
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
                            .flatMap(saved -> {
                                notifyOnlineDrivers(saved).subscribe();
                                return Mono.just(saved);
                            })
                            .doOnSuccess(o -> log.info("Offer created: {}", o.id()));
                });
    }

    private Mono<com.yowyob.rideandgo.domain.model.User> ensureUserExistsLocally(UUID userId) {
        return userRepositoryPort.findUserById(userId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("User {} not found locally. Syncing from Remote Auth...", userId);
                    return externalUserPort.fetchRemoteUserById(userId)
                            .flatMap(userRepositoryPort::save);
                }));
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
                        .flatMap(offer -> {
                            if (offer.hasDriverApplied(driverId))
                                return Mono.just(offer);

                            // CORRECTION : Gestion robuste de la liste (null-safe et mutable)
                            List<Bid> currentBids = offer.bids() != null
                                    ? new ArrayList<>(offer.bids())
                                    : new ArrayList<>();

                            currentBids.add(Bid.builder().driverId(driverId).build());

                            log.info("🚀 Driver {} applying to Offer {}. Total bids: {}", driverId, offerId,
                                    currentBids.size());

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
        log.info("Client selecting driver {} for offer {}", driverId, offerId);
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (!offer.hasDriverApplied(driverId)) {
                        return Mono.error(new IllegalArgumentException("Driver has not applied."));
                    }

                    Offer updated = offer.withDriverSelected(driverId);

                    return repository.save(updated)
                            .flatMap(saved -> cache.saveInCache(saved).thenReturn(saved))
                            .flatMap(saved -> {
                                // Save & Dispatch Notification
                                saveAndDispatch(
                                        driverId,
                                        tmplDriverSelected,
                                        "Course obtenue !",
                                        "Le client vous a sélectionné pour la course.",
                                        Map.of("offerId", offerId.toString(), "price", String.valueOf(offer.price())))
                                        .subscribe();
                                return Mono.just(saved);
                            })
                            .doOnSuccess(saved -> log.info("Offer {} updated to DRIVER_SELECTED for driver {}",
                                    saved.id(), driverId));
                });
    }

    // ==================================================================================
    // 5. CONFIRMATION & CRÉATION TRIP (CHAUFFEUR)
    // ==================================================================================
    // Note: Cette méthode est appelée directement par le Controller pour la
    // validation.
    public Mono<Ride> driverAcceptsOffer(UUID offerId, UUID driverId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.state() != OfferState.DRIVER_SELECTED) {
                        return Mono.error(new OfferStatutNotMatchException("Offer must be in DRIVER_SELECTED state."));
                    }
                    if (offer.selectedDriverId() == null || !offer.selectedDriverId().equals(driverId)) {
                        return Mono.error(new IllegalStateException("Access Denied: You are not the selected driver."));
                    }
                    return repository.save(offer.withState(OfferState.VALIDATED));
                })
                .flatMap(offer -> {
                    Ride ride = Ride.builder()
                            .id(Utils.generateUUID())
                            .offerId(offer.id())
                            .passengerId(offer.passengerId())
                            .driverId(driverId)
                            .state(RideState.CREATED)
                            .build();

                    return rideRepositoryPort.save(ride)
                            .flatMap(savedRide -> {
                                // Save & Dispatch Notification
                                saveAndDispatch(
                                        offer.passengerId(),
                                        tmplRideConfirmed,
                                        "Chauffeur en route",
                                        "Votre chauffeur a confirmé et arrive.",
                                        Map.of("rideId", savedRide.id().toString(), "driverId", driverId.toString()))
                                        .subscribe();
                                return Mono.just(savedRide);
                            });
                })
                .doOnSuccess(r -> log.info("Driver accepted. Ride created: {}", r.id()));
    }

    // ==================================================================================
    // 6. ANNULATION (PASSAGER)
    // ==================================================================================
    public Mono<Offer> cancelOffer(UUID offerId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    if (offer.state() == OfferState.VALIDATED) {
                        return Mono.error(
                                new IllegalStateException("Cannot cancel a validated offer. Cancel the ride instead."));
                    }

                    Mono<Offer> savedOperation = repository.save(offer.withState(OfferState.CANCELLED));

                    // Si un chauffeur était déjà sélectionné, il faut le prévenir
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
    // CENTRALISATION : SAUVEGARDE EN BD + ENVOI PUSH/EMAIL
    // ==================================================================================

    /**
     * Sauvegarde la notification dans l'historique utilisateur (DB) PUIS déclenche
     * l'envoi externe.
     */
    private Mono<Void> saveAndDispatch(UUID userId, int templateId, String title, String message,
            Map<String, Object> data) {
        // 1. Conversion de la map 'data' en JSON String pour stockage DB
        String json = "{}";
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("Failed to serialize notification data", e);
        }

        // 2. Création de l'objet Notification (Domaine)
        Notification history = Notification.builder()
                .id(Utils.generateUUID())
                .userId(userId)
                .title(title)
                .message(message)
                .type("INFO")
                .isRead(false)
                .dataJson(json)
                .build();

        // 3. Sauvegarde puis envoi
        return historyRepositoryPort.save(history)
                .then(dispatchNotification(userId, templateId, data));
    }

    /**
     * Envoie la notification à un utilisateur spécifique en respectant ses
     * préférences (Settings).
     */
    private Mono<Void> dispatchNotification(UUID userId, int templateId, Map<String, Object> data) {
        // 1. On récupère les préférences de l'utilisateur (ou les défauts)
        return settingsRepositoryPort.getSettings(userId)
                .flatMap(settings -> {
                    List<Mono<Boolean>> sendingTasks = new ArrayList<>();

                    // 2. Canal PUSH (Prioritaire)
                    if (settings.pushEnabled()) {
                        sendingTasks.add(
                                userDeviceRepositoryPort.findDeviceTokenByUserId(userId)
                                        .flatMap(token -> send(NotificationType.PUSH, templateId, List.of(token), data))
                                        // Si pas de token, on ignore silencieusement (ne fait pas planter le flux)
                                        .defaultIfEmpty(false));
                    }

                    // 3. Canal EMAIL
                    if (settings.emailEnabled()) {
                        sendingTasks.add(
                                userRepositoryPort.findUserById(userId)
                                        .flatMap(user -> send(NotificationType.EMAIL, templateId, List.of(user.email()),
                                                data)));
                    }

                    // 4. Canal SMS
                    if (settings.smsEnabled()) {
                        sendingTasks.add(
                                userRepositoryPort.findUserById(userId)
                                        .flatMap(user -> send(NotificationType.SMS, templateId,
                                                List.of(user.telephone()), data)));
                    }

                    // 5. Canal WHATSAPP
                    if (settings.whatsappEnabled()) {
                        sendingTasks.add(
                                userRepositoryPort.findUserById(userId)
                                        .flatMap(user -> send(NotificationType.WHATSAPP, templateId,
                                                List.of(user.telephone()), data)));
                    }

                    // Exécution parallèle de tous les envois
                    return Flux.merge(sendingTasks).then();
                });
    }

    /**
     * Helper générique pour appeler le port de notification.
     */
    private Mono<Boolean> send(NotificationType type, int tmpl, List<String> to, Map<String, Object> data) {
        return sendNotificationPort.sendNotification(
                SendNotificationRequest.builder()
                        .notificationType(type)
                        .templateId(tmpl)
                        .to(to)
                        .data(data)
                        .build());
    }

    /**
     * Cas spécial : Notification de masse aux chauffeurs en ligne.
     * Pour l'instant on envoie en PUSH uniquement (Broadcast) sans historisation
     * individuelle.
     */
    private Mono<Void> notifyOnlineDrivers(Offer offer) {
        return driverRepositoryPort.findDeviceTokensOfOnlineDrivers()
                .flatMap(token -> send(NotificationType.PUSH, tmplNewOffer, List.of(token), Map.of(
                        "offerId", offer.id().toString(),
                        "price", String.valueOf(offer.price()),
                        "start", offer.startPoint(),
                        "end", offer.endPoint())))
                .then();
    }

    // ==================================================================================
    // UTILITAIRES & ENRICHISSEMENT (POUR UI)
    // ==================================================================================

    // Pas d'Override car méthode utilitaire spécifique utilisée par le Controller
    public Mono<Offer> getOfferWithEnrichedBids(UUID offerId) {
        return repository.findById(offerId)
                .flatMap(offer -> {
                    log.info("here is the first log");
                    if (offer.bids() == null || offer.bids().isEmpty())
                        return Mono.just(offer);

                    log.info("here is the second log");
                    return Flux.fromIterable(offer.bids())
                            .flatMap(bid -> Mono.zip(
                                    userRepositoryPort.findUserById(bid.driverId()),
                                    locationCachePort.getLocation(bid.driverId())
                                            .defaultIfEmpty(new LocationCachePort.Location(0.0, 0.0)))
                                    .flatMap(tuple -> etaCalculatorService
                                            .calculateEta(tuple.getT2().latitude(), tuple.getT2().longitude(), 0.0, 0.0)
                                            .map(eta -> {
                                                // Injection de Fake Data pour la démo UI (Plaque, Modèle)
                                                DemoVehicle fakeCar = FAKE_FLEET
                                                        .get(ThreadLocalRandom.current().nextInt(FAKE_FLEET.size()));

                                                return Bid.builder()
                                                        .driverId(tuple.getT1().id())
                                                        .driverName(tuple.getT1().name())
                                                        .latitude(tuple.getT2().latitude())
                                                        .longitude(tuple.getT2().longitude())
                                                        .eta(eta)
                                                        .rating(4.5 + (Math.random() * 0.5))
                                                        .carModel(fakeCar.model())
                                                        .carColor(fakeCar.color())
                                                        .licensePlate(fakeCar.plate())
                                                        .driverImage(fakeCar.image())
                                                        .build();
                                            })))
                            .collectList()
                            .map(offer::withBids);
                });
    }

    // ==================================================================================
    // CRUD STANDARD (OfferManagementUseCase)
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
                    Offer updated = new Offer(
                            existingOffer.id(),
                            existingOffer.passengerId(),
                            existingOffer.selectedDriverId(),
                            offerDetails.startPoint() != null ? offerDetails.startPoint() : existingOffer.startPoint(),
                            offerDetails.endPoint() != null ? offerDetails.endPoint() : existingOffer.endPoint(),
                            offerDetails.price() > 0 ? offerDetails.price() : existingOffer.price(),
                            existingOffer.state(),
                            existingOffer.bids(),
                            existingOffer.version());
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