package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.SendNotificationRequest;
import reactor.core.publisher.Mono;

public interface SendNotificationPort {
    Mono<Boolean> sendNotification(SendNotificationRequest request);
}
