package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.ports.in.AcceptedOfferUseCase;
import com.yowyob.rideandgo.domain.ports.in.CreateOfferUseCase;
import com.yowyob.rideandgo.domain.ports.in.ResponseToOfferUseCase;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.CreateOfferRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.OfferResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.RideResponse;
import com.yowyob.rideandgo.infrastructure.mappers.OfferMapper;
import com.yowyob.rideandgo.infrastructure.mappers.RideMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/offer")
@Tag(name = "Offers", description = "Operations related to offers")
public class OfferController {
    private final CreateOfferUseCase createOfferUseCase;
    private final ResponseToOfferUseCase responseToOfferUseCase;
    private final AcceptedOfferUseCase acceptedOfferUseCase;
    private final OfferMapper mapper;
    private final RideMapper rideMapper;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "Create an offer", description = "Create a new offer for a given passenger")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer created", content = @Content(schema = @Schema(implementation = OfferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<OfferResponse> createOffer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Offer creation payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateOfferRequest.class)))
            @RequestBody CreateOfferRequest request) {
        Offer offer = mapper.toDomain(request);
        return createOfferUseCase.createOffer(offer, request.passengerId())
                .map(mapper::toResponse);
    }

    @PutMapping
    @Operation(summary = "Respond to an offer", description = "Driver responds to an existing offer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer updated", content = @Content(schema = @Schema(implementation = OfferResponse.class))),
            @ApiResponse(responseCode = "404", description = "Offer not found")
    })
    public Mono<OfferResponse> responseToOffer(
            @Parameter(description = "Offer identifier", required = true) @RequestParam UUID offerId,
            @Parameter(description = "Driver identifier", required = true) @RequestParam UUID driverId) {
        return responseToOfferUseCase.responseToOffer(offerId, driverId)
                .map(this::convertToOffer)
                .map(mapper::toResponse);
    }

    @PostMapping("/validate-offer")
    @Operation(summary = "Accept an offer", description = "Accept an offer to create a ride")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride created", content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "404", description = "Offer or users not found")
    })
    public Mono<RideResponse> acceptOffer(
            @Parameter(description = "Offer identifier", required = true) @RequestParam UUID offerId,
            @Parameter(description = "Passenger identifier", required = true) @RequestParam UUID passengerId,
            @Parameter(description = "Driver identifier", required = true) @RequestParam UUID driverId) {
        return acceptedOfferUseCase.acceptedOffer(offerId, passengerId, driverId)
                .map(rideMapper::toResponse);
    }

    private Offer convertToOffer(Object instance) {
        if (instance instanceof Offer offer) {
            return offer;
        }
        return objectMapper.convertValue(instance, Offer.class);
    }
}