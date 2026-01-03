package com.yowyob.rideandgo.infrastructure.config;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.AuthApiClient;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.FareCalculatorClient;

import reactor.core.publisher.Mono;

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
        
        // Configuration pour API Key (Header standard)
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
                .filter(addBearerToken()) 
                .build();

        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(AuthApiClient.class);
    }

    private <T> T createClient(WebClient.Builder builder, String url, Class<T> clientClass) {
        WebClient webClient = builder.baseUrl(url).build();
        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(clientClass);
    }
 
    /**
     * Filtre avec Logging pour diagnostiquer la perte de Token
     */
    private ExchangeFilterFunction addBearerToken() {
        return (request, next) -> ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication())
            .flatMap(auth -> {
                // LOG 1 : Voir si on a l'objet Authentication
                System.out.println("🔍 [DEBUG WebClient] Auth Principal: " + auth.getPrincipal());
                
                Object credentials = auth.getCredentials();
                // LOG 2 : Voir ce qu'il y a dans les credentials
                System.out.println("🔍 [DEBUG WebClient] Credentials Type: " + (credentials != null ? credentials.getClass().getName() : "NULL"));
                System.out.println("🔍 [DEBUG WebClient] Credentials Value: " + credentials);

                if (credentials instanceof String token) {
                    System.out.println("✅ [DEBUG WebClient] Token trouvé ! Injection dans le header...");
                    ClientRequest newRequest = ClientRequest.from(request)
                            .headers(headers -> headers.setBearerAuth(token))
                            .build();
                    return next.exchange(newRequest);
                } else {
                    System.out.println("⚠️ [DEBUG WebClient] Pas de Token String dans credentials. Envoi sans Auth.");
                }
                return next.exchange(request);
            })
            .switchIfEmpty(Mono.defer(() -> {
                System.out.println("❌ [DEBUG WebClient] Contexte de Sécurité VIDE ! (Pas d'utilisateur connecté ?)");
                return next.exchange(request);
            }));
    }
}