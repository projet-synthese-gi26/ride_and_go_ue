package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.ports.out.FareClientPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.FareCalculatorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class RemoteFareAdapter implements FareClientPort {
    private final FareCalculatorClient client;
    private final ReactiveCircuitBreakerFactory<?, ?> cbFactory;
    private final FareClientPort fallbackAdapter;

    @Override
    public Mono<FareResponse> caclculateFare(FareRequest request) {
        log.info("🌐 MODE REMOTE FARE : Calling external API for {} to {}", request.depart(), request.arrivee());
        ReactiveCircuitBreaker rcb = cbFactory.create("fare-calculator-service");

        return rcb.run(
                client.calculateFare(request),
                throwable -> {
                    log.warn("🚨 External Fare Service failed, using fallback: {}", throwable.getMessage());
                    return fallbackAdapter.caclculateFare(request);
                }
        );
    }
}