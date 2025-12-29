package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.ports.out.FareClientPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.FareCalculatorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
@RequiredArgsConstructor
public class FareAdapter implements FareClientPort {
    private final FareCalculatorClient client;
    private final ReactiveCircuitBreakerFactory<?, ?> cbFactory;

    @Override
    public Mono<FareResponse> caclculateFare(FareRequest request) {
        ReactiveCircuitBreaker rcb = cbFactory.create("fare-calculator-service");

        return rcb.run(
                client.calculateFare(request),
                throwable -> fallbackFareCheck(request, throwable)
        );
    }

    private Mono<FareResponse> fallbackFareCheck(FareRequest request, Throwable throwable) {
        log.warn("Circuit Breaker open or Service Down for {}. Error: {}", request, throwable.getMessage());

        return Mono.just(createRandomFareResponse());
    }

    private FareResponse createRandomFareResponse() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int distance = random.nextInt(1_000, 50_001);
        int duration = random.nextInt(300, 3_601);
        String currency = "EUR";
        String fareId = Utils.generateUUID().toString();
        boolean success = true;

        return new FareResponse(distance, duration, currency, fareId, success);
    }
}
