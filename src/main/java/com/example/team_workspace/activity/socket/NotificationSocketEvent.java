package com.example.team_workspace.activity.socket;

import com.example.team_workspace.activity.dto.NotificationResponse;

/**
 * Envelope pushed to {@code /user/queue/notifications}. The {@code type} field
 * is the discriminator; {@code payload} is interpreted according to it:
 *
 * <ul>
 *   <li>{@link NotificationEventType#NEW_NOTIFICATION} - a notification object</li>
 *   <li>{@link NotificationEventType#NOTIFICATION_READ} - the updated notification</li>
 *   <li>{@link NotificationEventType#ALL_READ} - the number of rows updated</li>
 * </ul>
 *
 * <p>Every event carries the recipient's authoritative unread count so the badge
 * never has to be recomputed or refetched on the client.
 *
 * <p>{@code payload} is typed as {@link Object} because the three variants differ
 * in shape. The client must narrow on {@code type} before reading it.
 */
public record NotificationSocketEvent(
        NotificationEventType type,
        Object payload,
        long unreadCount
) {

    /** Destination suffix; the user prefix {@code /user} is added by the broker. */
    public static final String DESTINATION = "/queue/notifications";

    public static NotificationSocketEvent newNotification(
            NotificationResponse notification,
            long unreadCount
    ) {
        return new NotificationSocketEvent(
                NotificationEventType.NEW_NOTIFICATION,
                notification,
                unreadCount
        );
    }

    public static NotificationSocketEvent notificationRead(
            NotificationResponse notification,
            long unreadCount
    ) {
        return new NotificationSocketEvent(
                NotificationEventType.NOTIFICATION_READ,
                notification,
                unreadCount
        );
    }

    public static NotificationSocketEvent allRead(int updatedCount, long unreadCount) {
        return new NotificationSocketEvent(NotificationEventType.ALL_READ, updatedCount, unreadCount);
    }
}
