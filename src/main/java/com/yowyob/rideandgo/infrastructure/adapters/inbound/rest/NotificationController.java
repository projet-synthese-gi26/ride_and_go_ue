package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.model.Notification;
import com.yowyob.rideandgo.domain.ports.in.MarkNotificationAsReadUseCase;
import com.yowyob.rideandgo.domain.ports.out.NotificationHistoryRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification-History", description = "In-app notifications history")
public class NotificationController {
    private final NotificationHistoryRepositoryPort historyPort;
    private final MarkNotificationAsReadUseCase markAsReadUseCase;

    @GetMapping
    @Operation(summary = "Get my notifications", description = "Paginated list of notifications for the connected user.")
    public Mono<NotificationHistoryRepositoryPort.PagedResult<Notification>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return getCurrentUserId()
                .flatMap(userId -> historyPort.getUserNotifications(userId, page, size));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public Mono<Void> markAsRead(@PathVariable UUID id) {
        return getCurrentUserId()
                .flatMap(userId -> markAsReadUseCase.markAsRead(id, userId));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public Mono<Void> markAllAsRead() {
        return getCurrentUserId()
                .flatMap(markAsReadUseCase::markAllAsRead);
    }

    private Mono<UUID> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    try {
                        return Mono.just(UUID.fromString(auth.getName()));
                    } catch (Exception e) {
                        return Mono.error(new IllegalStateException("Invalid User Context"));
                    }
                });
    }
}