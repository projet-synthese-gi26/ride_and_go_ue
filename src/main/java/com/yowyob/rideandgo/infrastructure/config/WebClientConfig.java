package com.yowyob.rideandgo.infrastructure.config;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.AuthApiClient;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.FareCalculatorClient;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.NotificationApiClient;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.PaymentApiClient;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.SyndicateApiClient;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.VehicleApiClient;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
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

    private final HttpClient httpClient = HttpClient.create().wiretap(true);

    @Bean
    public PaymentApiClient paymentApiClient(WebClient.Builder builder,
            @Value("${application.payment.url}") String url) {
        WebClient webClient = builder.baseUrl(url).build();
        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        return HttpServiceProxyFactory.builderFor(adapter).build().createClient(PaymentApiClient.class);
    }

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
        
        // On applique le connector Netty au builder
        WebClient webClient = builder
                .baseUrl(url)
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // Injection Netty
                .filter(addBearerToken()) 
                .filter(logRequest()) 
                .build();

        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(AuthApiClient.class);
    }

    private ExchangeFilterFunction logRequest() {
        return (request, next) -> {
            // ✅ LOG ÉLARGI : On loggue tout ce qui va vers le service Auth pour debug
            log.info("🚀 [WebClient Outbound] {} {}", request.method(), request.url());
            request.headers().forEach((name, values) -> 
                values.forEach(value -> {
                    String maskedValue = name.equalsIgnoreCase("Authorization") ? "Bearer ********" : value;
                    log.info("   🧩 Header: {}={}", name, maskedValue);
                })
            );
            return next.exchange(request);
        };
    }

    @Bean
    public NotificationApiClient notificationApiClient(WebClient.Builder builder,
            @Value("${application.notification.url}") String url,
            @Value("${application.notification.service-token}") String serviceToken) {
        WebClient webClient = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Token", serviceToken)
                .build();

        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        return HttpServiceProxyFactory.builderFor(adapter).build().createClient(NotificationApiClient.class);
    }

    @Bean
    public SyndicateApiClient syndicateApiClient(WebClient.Builder builder,
            @Value("${application.syndicate.url}") String url) {
        WebClient webClient = builder.baseUrl(url).filter(addBearerToken()).build(); 
        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        return HttpServiceProxyFactory.builderFor(adapter).build().createClient(SyndicateApiClient.class);
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

            // Ne pas ajouter de token pour les routes publiques d'auth
            if (path.contains("/auth/login") || path.contains("/auth/register") || path.contains("/auth/refresh")) {
                return next.exchange(request);
            }

            return ReactiveSecurityContextHolder.getContext()
                    .map(ctx -> ctx.getAuthentication())
                    .flatMap(auth -> {
                        Object credentials = auth.getCredentials();
                        if (credentials instanceof String token) {
                            ClientRequest newRequest = ClientRequest.from(request)
                                    .headers(headers -> headers.setBearerAuth(token))
                                    .build();
                            return next.exchange(newRequest);
                        }
                        return next.exchange(request);
                    })
                    // ✅ CRITIQUE : Si le contexte est vide (ex: appel swagger sans login), on loggue l'absence de token
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("⚠️ [WebClient] No Security Context found for path: {}. Sending without token.", path);
                        return next.exchange(request);
                    }));
        };
    }
}