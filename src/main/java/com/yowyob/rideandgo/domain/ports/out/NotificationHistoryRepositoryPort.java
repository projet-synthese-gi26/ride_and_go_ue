package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Notification;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.UUID;

public interface NotificationHistoryRepositoryPort {
    Mono<Void> save(Notification notification);

    Mono<PagedResult<Notification>> getUserNotifications(UUID userId, int page, int size);

    Mono<Void> markAsRead(UUID notificationId);

    Mono<Void> markAllAsReadForUser(UUID userId);

    // Record Helper pour la pagination
    record PagedResult<T>(List<T> content, long totalElements, int totalPages, int currentPage) {
    }
}