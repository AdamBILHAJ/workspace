package com.example.team_workspace.activity.dto;

import java.time.Instant;

import com.example.team_workspace.activity.domain.Notification;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        boolean isRead,
        String targetUrl,
        Instant createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getTargetUrl(),
                notification.getCreatedAt()
        );
    }
}
