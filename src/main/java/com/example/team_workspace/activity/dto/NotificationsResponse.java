package com.example.team_workspace.activity.dto;

import java.util.List;

public record NotificationsResponse(
        List<NotificationResponse> notifications,
        long unreadCount
) {

    public NotificationsResponse {
        notifications = List.copyOf(notifications);
    }
}
