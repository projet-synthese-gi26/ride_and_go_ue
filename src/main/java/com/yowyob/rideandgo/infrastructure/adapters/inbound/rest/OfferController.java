package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.ports.in.*;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.CreateOfferRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.OfferResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.RideResponse;
import com.yowyob.rideandgo.infrastructure.mappers.OfferMapper;
import com.yowyob.rideandgo.infrastructure.mappers.RideMapper;
import com.yowyob.rideandgo.application.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private final OfferMapper mapper;
    private final RideMapper rideMapper; 

    @PostMapping
    @Operation(summary = "Publish an offer (Passenger)")
    public Mono<OfferResponse> createOffer(@RequestBody CreateOfferRequest request) {
        return createOfferUseCase.createOffer(mapper.toDomain(request), request.passengerId())
                .map(mapper::toResponse);
    }

    @GetMapping("/available")
    @Operation(summary = "List pending offers (Driver)")
    public Flux<OfferResponse> getAvailable() {
        return getAvailableOffersUseCase.getAvailableOffers().map(mapper::toResponse);
    }

    @PostMapping("/{id}/apply")
    @Operation(summary = "Apply to an offer (Driver)")
    public Mono<OfferResponse> apply(@PathVariable UUID id, @RequestParam UUID driverId) {
        return responseToOfferUseCase.responseToOffer(id, driverId).map(mapper::toResponse);
    }

    @GetMapping("/{id}/bids")
    @Operation(summary = "Review enriched bidders (Passenger)")
    public Mono<OfferResponse> getBids(@PathVariable UUID id) {
        return offerService.getOfferWithEnrichedBids(id).map(mapper::toResponse);
    }

    @PatchMapping("/{id}/select-driver")
    @Operation(summary = "1. Passenger selects driver", description = "Offer state -> DRIVER_SELECTED")
    public Mono<OfferResponse> select(@PathVariable UUID id, @RequestParam UUID driverId) {
        return selectDriverUseCase.selectDriver(id, driverId).map(mapper::toResponse);
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "2. Driver confirms pickup", description = "Offer state -> VALIDATED. Creates Ride.")
    public Mono<RideResponse> driverAccepts(@PathVariable UUID id, @RequestParam UUID driverId) {
        return offerService.driverAcceptsOffer(id, driverId).map(rideMapper::toResponse);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel offer (Passenger)")
    public Mono<OfferResponse> cancel(@PathVariable UUID id) {
        return offerService.cancelOffer(id).map(mapper::toResponse);
    }
}