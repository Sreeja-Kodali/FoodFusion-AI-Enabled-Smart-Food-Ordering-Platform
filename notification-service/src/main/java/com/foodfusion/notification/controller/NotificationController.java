package com.foodfusion.notification.controller;

import com.foodfusion.notification.model.OrderNotification;
import com.foodfusion.notification.repository.OrderNotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final OrderNotificationRepository notifications;

    public NotificationController(OrderNotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<OrderNotification> getMyNotifications(
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        return notifications.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/orders/{orderNumber}")
    public List<OrderNotification> getOrderNotifications(
            @PathVariable String orderNumber,
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @RequestHeader(name = "X-User-Roles", required = false) String roles
    ) {
        List<OrderNotification> orderNotifications =
                notifications.findByOrderNumberOrderByCreatedAtDesc(orderNumber);
        boolean isAdmin = roles != null && List.of(roles.split(",")).contains("ADMIN");
        if (!isAdmin && (userId == null || orderNotifications.stream()
                .anyMatch(notification -> !userId.equals(notification.getUserId())))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only access your own notifications.");
        }
        return orderNotifications;
    }
}
