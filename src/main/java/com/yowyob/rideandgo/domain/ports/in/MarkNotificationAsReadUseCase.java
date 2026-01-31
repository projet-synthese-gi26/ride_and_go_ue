package com.yowyob.rideandgo.domain.ports.in;

import reactor.core.publisher.Mono;
import java.util.UUID;

public interface MarkNotificationAsReadUseCase {
    /**
     * Marque une notification spécifique comme lue.
     * 
     * @param notificationId ID de la notification
     * @param userId         ID de l'utilisateur (pour vérification de propriété)
     */
    Mono<Void> markAsRead(UUID notificationId, UUID userId);

    /**
     * Marque toutes les notifications d'un utilisateur comme lues.
     */
    Mono<Void> markAllAsRead(UUID userId);
}