package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Notification;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.UUID;

public interface NotificationHistoryRepositoryPort {
    Mono<Void> save(Notification notification);
    
    // Retourne une structure paginée simplifiée
    Mono<PagedResult<Notification>> getUserNotifications(UUID userId, int page, int size);

    // Record Helper pour la pagination
    record PagedResult<T>(List<T> content, long totalElements, int totalPages, int currentPage) {}
}