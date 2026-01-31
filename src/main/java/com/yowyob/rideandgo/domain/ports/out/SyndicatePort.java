package com.yowyob.rideandgo.domain.ports.out;

import reactor.core.publisher.Mono;
import java.util.UUID;

public interface SyndicatePort {
    Mono<Boolean> checkIsSyndicated(UUID userId);
}