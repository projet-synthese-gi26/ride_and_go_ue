package com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;

@HttpExchange("/api/stock")
public interface StockApiClient {
    @GetExchange("/check")
    Mono<StockResponse> checkStock(@RequestParam("name") String name);

    record StockResponse(boolean full) {}
}