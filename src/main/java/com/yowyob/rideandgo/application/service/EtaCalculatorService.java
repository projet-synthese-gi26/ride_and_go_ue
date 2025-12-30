package com.yowyob.rideandgo.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class EtaCalculatorService {

    /**
     * Logic: Random value between 3 and 12 minutes for MVP.
     */
    public Mono<Integer> calculateEta(Double startLat, Double startLon, Double endLat, Double endLon) {
        return Mono.fromCallable(() -> ThreadLocalRandom.current().nextInt(3, 13));
    }
}