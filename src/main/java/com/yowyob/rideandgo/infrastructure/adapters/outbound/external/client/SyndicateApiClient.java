package com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;

@HttpExchange("/compliance")
public interface SyndicateApiClient {
    @GetExchange("/details/{driverId}")
    Mono<SyndicateDetailsResponse> getDriverComplianceDetails(@PathVariable("driverId") String driverId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SyndicateDetailsResponse(
            String id,
            String firstName,
            String lastName,
            String licenseNumber,
            boolean isVerified) {
    }
}