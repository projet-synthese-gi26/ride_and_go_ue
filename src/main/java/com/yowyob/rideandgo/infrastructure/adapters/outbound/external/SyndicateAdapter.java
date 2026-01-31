package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.ports.out.SyndicatePort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.SyndicateApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyndicateAdapter implements SyndicatePort {
    private final SyndicateApiClient client;

    @Override
    public Mono<Boolean> checkIsSyndicated(UUID userId) {
        log.info("🔍 Checking Syndicate status for user {} at UGate", userId);
        return client.getDriverComplianceDetails(userId.toString())
                .map(SyndicateApiClient.SyndicateDetailsResponse::isVerified)
                .onErrorResume(e -> {
                    log.error("❌ Failed to contact Syndicate service: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}