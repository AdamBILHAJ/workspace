package com.example.team_workspace.activity.socket;

/** Body of a {@code /app/notifications.read} STOMP message. */
public record MarkNotificationReadRequest(Long id) {
}
