package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.ports.out.FareClientPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fares")
@Tag(name = "Fare-Calculator", description = "Trip price estimation (Stateless)")
public class FareController {

    private final FareClientPort fareClientPort;

    @PostMapping("/estimate")
    @Operation(summary = "Calculate fare estimation", description = "Returns suggested price. No data persistence.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estimation successful", 
                        content = @Content(schema = @Schema(implementation = FareResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public Mono<FareResponse> estimateFare(@RequestBody FareRequest request) {
        return fareClientPort.caclculateFare(request);
    }
}