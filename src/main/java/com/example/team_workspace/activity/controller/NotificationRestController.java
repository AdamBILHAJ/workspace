package com.example.team_workspace.activity.controller;

import java.util.Map;

import com.example.team_workspace.activity.dto.NotificationResponse;
import com.example.team_workspace.activity.dto.NotificationsResponse;
import com.example.team_workspace.activity.service.NotificationService;
import com.example.team_workspace.user.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationRestController {

    private final NotificationService notificationService;

    public NotificationRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationsResponse> listNotifications(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(notificationService.listNotifications(currentUser));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUser));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(Map.of("updatedCount", notificationService.markAllAsRead(currentUser)));
    }
}
