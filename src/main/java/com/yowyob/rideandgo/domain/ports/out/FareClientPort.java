package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import reactor.core.publisher.Mono;

public interface FareClientPort {
    Mono<FareResponse> caclculateFare(FareRequest request);
}
