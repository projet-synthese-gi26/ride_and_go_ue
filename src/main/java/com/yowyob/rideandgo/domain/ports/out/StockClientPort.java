package com.yowyob.rideandgo.domain.ports.out;

import reactor.core.publisher.Mono;

public interface StockClientPort {
    /**
     * Verify if te stock is full.
     * @return true full, false otherwise
     */
    Mono<Boolean> isStockFull(String productName);
}