package com.yowyob.rideandgo.infrastructure.config;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.AuthApiClient;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.FareCalculatorClient;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.NotificationApiClient;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.VehicleApiClient;

import reactor.core.publisher.Mono;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public FareCalculatorClient fareCalculatorClient(WebClient.Builder builder,
            @Value("${application.fare.url}") String url,
            @Value("${application.fare.api-key}") String apiKey) {

        WebClient webClient = builder
                .baseUrl(url)
                .defaultHeader("Authorization", "ApiKey " + apiKey)
                .build();

        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        return HttpServiceProxyFactory.builderFor(adapter).build().createClient(FareCalculatorClient.class);
    }

    @Bean
    public AuthApiClient authApiClient(WebClient.Builder builder, 
                                       @Value("${application.auth.url}") String url) {
        WebClient webClient = builder
                .baseUrl(url)
                .filter(addBearerToken()) // Gestion Token Bearer
                .filter(logRequest())     // Ajout Logging
                .build();

        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(AuthApiClient.class);
    }

    private ExchangeFilterFunction logRequest() {
        return (request, next) -> {
            // Log uniquement pour les routes Auth pour ne pas polluer
            if (request.url().getPath().contains("/auth/")) {
                log.info("🚀 [WebClient] Request: {} {}", request.method(), request.url());
                request.headers().forEach((name, values) -> 
                    values.forEach(value -> log.info("   🧩 Header: {}={}", name, value))
                );
            }
            return next.exchange(request);
        };
    }

    @Bean
    public NotificationApiClient notificationApiClient(WebClient.Builder builder,
            @Value("${application.notification.url}") String url,
            @Value("${application.notification.service-token}") String serviceToken) {
        // Configuration pour le service de notification avec X-Service-Token
        WebClient webClient = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Token", serviceToken)
                .build();

        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        return HttpServiceProxyFactory.builderFor(adapter).build().createClient(NotificationApiClient.class);
    }

    @Bean
    public VehicleApiClient vehicleApiClient(WebClient.Builder builder,
            @Value("${application.vehicle.url}") String url) {
        WebClient webClient = builder.baseUrl(url).filter(addBearerToken()).build();
        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        return HttpServiceProxyFactory.builderFor(adapter).build().createClient(VehicleApiClient.class);
    }

    private ExchangeFilterFunction addBearerToken() {
        return (request, next) -> {
            URI url = request.url();
            String path = url.getPath();

            // ⛔ EXCLUSION : On ne transmet jamais le Bearer Token local vers les routes
            // Auth distantes
            // car cela crée un conflit (401) si le service distant valide le header avant
            // le body.
            if (path.contains("/auth/login") || path.contains("/auth/register") || path.contains("/auth/refresh")) {
                System.out.println("⏩ [WebClient] Auth endpoint detecté (" + path + "). Pas d'injection de token.");
                return next.exchange(request);
            }

            return ReactiveSecurityContextHolder.getContext()
                    .map(ctx -> ctx.getAuthentication())
                    .flatMap(auth -> {
                        Object credentials = auth.getCredentials();
                        if (credentials instanceof String token) {
                            // System.out.println("✅ [WebClient] Injection Token pour : " + path);
                            ClientRequest newRequest = ClientRequest.from(request)
                                    .headers(headers -> headers.setBearerAuth(token))
                                    .build();
                            return next.exchange(newRequest);
                        }
                        return next.exchange(request);
                    })
                    .switchIfEmpty(Mono.defer(() -> next.exchange(request)));
        };
    }
}