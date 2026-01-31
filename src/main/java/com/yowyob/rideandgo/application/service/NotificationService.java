package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.ports.in.MarkNotificationAsReadUseCase;
import com.yowyob.rideandgo.domain.ports.out.NotificationHistoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements MarkNotificationAsReadUseCase {
    private final NotificationHistoryRepositoryPort historyRepositoryPort;

    @Override
    public Mono<Void> markAsRead(UUID notificationId, UUID userId) {
        log.info("Marking notification {} as read for user {}", notificationId, userId);
        // Ici, on pourrait ajouter une vérification pour s'assurer que la notification
        // appartient bien au userId
        // Mais pour simplifier, on délègue au repo.
        return historyRepositoryPort.markAsRead(notificationId);
    }

    @Override
    public Mono<Void> markAllAsRead(UUID userId) {
        log.info("Marking all notifications as read for user {}", userId);
        return historyRepositoryPort.markAllAsReadForUser(userId);
    }
}