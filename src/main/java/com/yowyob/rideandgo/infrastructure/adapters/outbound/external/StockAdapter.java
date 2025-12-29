package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.ports.out.StockClientPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.StockApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class StockAdapter implements StockClientPort {

    private final StockApiClient client;
    private final ReactiveCircuitBreakerFactory<?, ?> cbFactory;

    @Override
    public Mono<Boolean> isStockFull(String productName) {
        ReactiveCircuitBreaker rcb = cbFactory.create("stock-service");

        return rcb.run(
                client.checkStock(productName).map(StockApiClient.StockResponse::full),
                throwable -> fallbackStockCheck(productName, throwable)
        );
    }

    private Mono<Boolean> fallbackStockCheck(String productName, Throwable t) {
        log.warn("Circuit Breaker open or Service Down for {}. Error: {}", productName, t.getMessage());
        // Fallback: if the service is down, consider the stock not full
        return Mono.just(false);
    }
}