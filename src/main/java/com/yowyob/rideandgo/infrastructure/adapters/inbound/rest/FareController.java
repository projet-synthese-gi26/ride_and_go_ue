package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.ports.in.PutFareInCacheUseCase;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.FareCalculatorClient;
import com.yowyob.rideandgo.infrastructure.mappers.FareMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fares")
@Tag(name = "Fares", description = "Trip price estimation")
public class FareController {
    private final FareCalculatorClient client;
    private final FareMapper mapper;
    private final PutFareInCacheUseCase putFareInCacheUseCase;

    @PostMapping
    @Operation(summary = "Calculate fare", description = "Calculate fare and optionally cache the result")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fare calculated", content = @Content(schema = @Schema(implementation = FareResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public Mono<FareResponse> calculateFare(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fare calculation request", required = true,
                    content = @Content(schema = @Schema(implementation = FareRequest.class)))
            @RequestBody FareRequest request) {
        return client.calculateFare(request)
                .map(mapper::toDomain)
                .flatMap(fare -> putFareInCacheUseCase.putFareInCache(fare)
                        .map(isCached -> mapper.toResponse(fare, isCached))
                        .onErrorReturn(mapper.toResponse(fare, false))
                );
    }
}
