package com.yowyob.rideandgo.infrastructure.adapters.outbound.messaging;

import com.yowyob.rideandgo.application.utils.Constants;
import com.yowyob.rideandgo.domain.ports.out.SendNotificationPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendNotificationAdapter implements SendNotificationPort {

    private final KafkaTemplate<String, SendNotificationRequest> kafkaTemplate;

    @Value("${application.kafka.notification-service.token}")
    private String token;

    @Value("${application.kafka.topics.notification-service-create-topic}")
    private String topic;

    @Override
    public Mono<Boolean> sendNotification(SendNotificationRequest request) {
        ProducerRecord<String, SendNotificationRequest> record = new ProducerRecord<>(topic, request);
        record.headers().add(Constants.NOTIFICATION_SERVICE_TOKEN_HEADER, token.getBytes());

        return Mono.fromFuture(kafkaTemplate.send(record))
                .thenReturn(true)
                .onErrorReturn(false);
    }
}
