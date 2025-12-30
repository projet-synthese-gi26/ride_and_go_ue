package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.ports.in.UpdateLocationUseCase;
import com.yowyob.rideandgo.domain.ports.out.LocationCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService implements UpdateLocationUseCase {

    private final LocationCachePort locationCachePort;

    @Override
    public Mono<Boolean> updateCurrentLocation(Double latitude, Double longitude) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    String userIdStr = auth.getName(); // Extracted from JWT
                    try {
                        UUID userId = UUID.fromString(userIdStr);
                        return locationCachePort.saveLocation(userId, latitude, longitude);
                    } catch (IllegalArgumentException e) {
                        log.error("Security Context principal is not a valid UUID: {}", userIdStr);
                        return Mono.just(false);
                    }
                })
                .switchIfEmpty(Mono.error(new RuntimeException("No security context found for location update")));
    }
}