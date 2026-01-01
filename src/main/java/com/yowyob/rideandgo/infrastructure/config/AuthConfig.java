package com.yowyob.rideandgo.infrastructure.config;

import com.yowyob.rideandgo.domain.ports.out.AuthPort;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.FakeAuthAdapter;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.RemoteAuthAdapter;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.AuthApiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

    @Bean
    @ConditionalOnProperty(name = "application.auth.mode", havingValue = "fake")
    public AuthPort fakeAuthPort() {
        return new FakeAuthAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "application.auth.mode", havingValue = "remote", matchIfMissing = true)
    public AuthPort remoteAuthPort(AuthApiClient authApiClient, UserRepositoryPort userRepositoryPort) {
        // Injection du repository pour la sauvegarde locale des utilisateurs lors du register
        return new RemoteAuthAdapter(authApiClient, userRepositoryPort);
    }
}