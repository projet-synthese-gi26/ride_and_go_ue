package com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client;


import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

@HttpExchange("/api")
public interface FareCalculatorClient {
    @PostExchange("/estimate/")
    Mono<FareResponse> calculateFare(@RequestBody FareRequest request);
}
