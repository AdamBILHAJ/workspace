package com.example.team_workspace.activity.socket;

/**
 * Discriminator for the notification WebSocket envelope. The client switches on
 * this to decide how to apply {@link NotificationSocketEvent#payload()}.
 */
public enum NotificationEventType {
    /** A notification was just created. Payload is a notification object. */
    NEW_NOTIFICATION,
    /** One notification was marked read. Payload is the updated notification. */
    NOTIFICATION_READ,
    /** Every notification was marked read. Payload is the number of rows updated. */
    ALL_READ
}
