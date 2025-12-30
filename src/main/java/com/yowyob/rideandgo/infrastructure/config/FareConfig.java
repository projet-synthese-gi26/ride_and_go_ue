package com.yowyob.rideandgo.infrastructure.config;

import com.yowyob.rideandgo.domain.ports.out.FareClientPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.FakeFareAdapter;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.RemoteFareAdapter;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.FareCalculatorClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class FareConfig {

    /**
     * Always available as a candidate for fallback.
     */
    @Bean
    public FareClientPort fakeFareAdapter() {
        return new FakeFareAdapter();
    }

    /**
     * Active only when mode is 'remote'.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "application.fare.mode", havingValue = "remote")
    public FareClientPort remoteFareAdapter(FareCalculatorClient client, 
                                           ReactiveCircuitBreakerFactory<?, ?> cbFactory) {
        return new RemoteFareAdapter(client, cbFactory, fakeFareAdapter());
    }

    /**
     * Active only when mode is 'fake'.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "application.fare.mode", havingValue = "fake", matchIfMissing = true)
    public FareClientPort onlyFakeFareAdapter() {
        return fakeFareAdapter();
    }
}