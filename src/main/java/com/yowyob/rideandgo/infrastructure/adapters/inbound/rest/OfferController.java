package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.ports.in.*;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.CreateOfferRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.OfferResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.RideResponse;
import com.yowyob.rideandgo.infrastructure.mappers.OfferMapper;
import com.yowyob.rideandgo.infrastructure.mappers.RideMapper;
import com.yowyob.rideandgo.application.service.OfferService;
import com.yowyob.rideandgo.application.service.RideService; // Import ajouté
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UpdateOfferRequest;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/offers")
@Tag(name = "Offer-Controller", description = "Matchmaking workflow")
public class OfferController {
    
    private final CreateOfferUseCase createOfferUseCase;
    private final GetAvailableOffersUseCase getAvailableOffersUseCase;
    private final ResponseToOfferUseCase responseToOfferUseCase;
    private final SelectDriverUseCase selectDriverUseCase;
    private final OfferService offerService; 
    private final RideService rideService; // Injection ajoutée
    private final OfferMapper mapper;
    private final RideMapper rideMapper; 

    @PostMapping
    @Operation(summary = "Publish an offer (Passenger)")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_PASSENGER')") 
    public Mono<OfferResponse> createOffer(@RequestBody CreateOfferRequest request) {
        // Extraction sécurisée de l'ID du passager depuis le token
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    try {
                        System.err.println(auth.getName());
                        UUID passengerId = UUID.fromString(auth.getName());
                        return createOfferUseCase.createOffer(mapper.toDomain(request), passengerId);
                    } catch (Exception e) {
                        return Mono.error(new IllegalStateException("Invalid User ID in Token"));
                    }
                })
                .map(mapper::toResponse);
    }

    @GetMapping("/available")
    @Operation(summary = "List pending offers (Driver)")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_DRIVER')")
    public Flux<OfferResponse> getAvailable() {
        return getAvailableOffersUseCase.getAvailableOffers().map(mapper::toResponse);
    }

    @PostMapping("/{id}/apply")
    @Operation(summary = "Apply to an offer (Driver)")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_DRIVER')")
    public Mono<OfferResponse> apply(@PathVariable UUID id) {
        // On récupère aussi le driverId depuis le token, plus besoin de param
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    UUID driverId = UUID.fromString(auth.getName());
                    return responseToOfferUseCase.responseToOffer(id, driverId);
                })
                .map(mapper::toResponse);
    }

    @GetMapping("/{id}/bids")
    @Operation(summary = "Review enriched bidders (Passenger)")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_PASSENGER')")
    public Mono<OfferResponse> getBids(@PathVariable UUID id) {
        return offerService.getOfferWithEnrichedBids(id).map(mapper::toResponse);
    }

    @PatchMapping("/{id}/select-driver")
    @Operation(summary = "1. Passenger selects driver", description = "Offer state -> DRIVER_SELECTED")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_PASSENGER')")
    public Mono<OfferResponse> select(@PathVariable UUID id, @RequestParam UUID driverId) {
        return selectDriverUseCase.selectDriver(id, driverId).map(mapper::toResponse);
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "2. Driver confirms pickup", description = "Offer state -> VALIDATED. Creates Ride.")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_DRIVER')")
    public Mono<RideResponse> driverAccepts(@PathVariable UUID id, @RequestParam UUID driverId) {
        return offerService.driverAcceptsOffer(id, driverId).map(rideMapper::toResponse);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel offer (Passenger)")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_PASSENGER')")
    public Mono<OfferResponse> cancel(@PathVariable UUID id) {
        return offerService.cancelOffer(id).map(mapper::toResponse);
    }

    // --- NOUVEAU ENDPOINT : Transition Offre -> Course ---
    @GetMapping("/{id}/ride")
    @Operation(summary = "Get linked ride for an offer", description = "Returns the Ride object if the offer has been validated by a driver.")
    public Mono<RideResponse> getRideByOfferId(@PathVariable UUID id) {
        return rideService.getRideByOfferId(id)
                .map(rideMapper::toResponse);
    }

    // --- ENDPOINTS DE GESTION / DEBUG ---

    @GetMapping
    @Operation(summary = "Get all offers (Admin/Debug)", description = "Retrieves all offers regardless of status")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_ADMIN')")
    public Flux<OfferResponse> getAllOffers() {
        return offerService.getAllOffers().map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get offer by ID", description = "Get details of a specific offer")
    public Mono<OfferResponse> getOfferById(@PathVariable UUID id) {
        return offerService.getOfferById(id).map(mapper::toResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update offer", description = "Modifies start/end points or price. Does not change state.")
    public Mono<OfferResponse> updateOffer(@PathVariable UUID id, @RequestBody UpdateOfferRequest request) {
        com.yowyob.rideandgo.domain.model.Offer domainUpdate = com.yowyob.rideandgo.domain.model.Offer.builder()
                .startPoint(request.startPoint())
                .endPoint(request.endPoint())
                .price(request.price() != null ? request.price() : 0.0)
                .build();
        
        return offerService.updateOffer(id, domainUpdate).map(mapper::toResponse);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete offer", description = "Permanently removes an offer")
    public Mono<Void> deleteOffer(@PathVariable UUID id) {
        return offerService.deleteOffer(id).then();
    }
}