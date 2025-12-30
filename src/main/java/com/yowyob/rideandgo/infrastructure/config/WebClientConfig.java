package com.yowyob.rideandgo.infrastructure.config;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.AuthApiClient;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.FareCalculatorClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {

    @Bean
    public FareCalculatorClient fareCalculatorClient(WebClient.Builder builder,
                                         @Value("${application.external.fare-calculator-url:https://fare-calculator-service.pynfi.com}") String url) {
        return createClient(builder, url, FareCalculatorClient.class);
    }

    /**
     * Client for the external Authentication Service (TraMaSys).
     */
    @Bean
    public AuthApiClient authApiClient(WebClient.Builder builder, 
                                       @Value("${application.auth.url:https://auth-service.pynfi.com}") String url) {
        return createClient(builder, url, AuthApiClient.class);
    }

    /**
     * Generic helper to build declarative HTTP clients.
     */
    private <T> T createClient(WebClient.Builder builder, String url, Class<T> clientClass) {
        WebClient webClient = builder.baseUrl(url).build();
        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(clientClass);
    }
}