package com.example.team_workspace.activity.socket;

import java.security.Principal;

import com.example.team_workspace.activity.dto.NotificationResponse;
import com.example.team_workspace.activity.service.NotificationService;
import com.example.team_workspace.auth.dto.ApiErrorResponse;
import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.websocket.StompJwtChannelInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Client-initiated notification actions over STOMP.
 *
 * <p>Each handler re-derives the recipient from the session Principal and echoes
 * the result to that Principal's own {@code /user/queue/notifications}. Because
 * user destinations fan out to every session bound to the Principal, one
 * "mark all read" updates the badge in all of the user's tabs at once.
 */
@Controller
public class NotificationStompController {

    private static final String ERROR_DESTINATION = "/queue/notifications/errors";

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationStompController(
            NotificationService notificationService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/notifications.read")
    public void markAsRead(@Payload MarkNotificationReadRequest request, Principal principal) {
        if (request == null || request.id() == null) {
            throw new IllegalArgumentException("A notification id is required");
        }

        User user = StompJwtChannelInterceptor.resolveUser(principal);
        NotificationResponse updated = notificationService.markAsRead(request.id(), user);
        long unread = notificationService.countUnread(user);

        publish(principal, NotificationSocketEvent.notificationRead(updated, unread));
    }

    @MessageMapping("/notifications.readAll")
    public void markAllAsRead(Principal principal) {
        User user = StompJwtChannelInterceptor.resolveUser(principal);
        int updated = notificationService.markAllAsRead(user);
        // Read back rather than assuming zero, so the badge stays correct even
        // if a notification arrives between the update and the response.
        long unread = notificationService.countUnread(user);

        publish(principal, NotificationSocketEvent.allRead(updated, unread));
    }

    /**
     * Failures are reported to the originating user instead of being swallowed,
     * which would otherwise leave the client optimistically waiting forever.
     */
    @MessageExceptionHandler
    @SendToUser(ERROR_DESTINATION)
    public ApiErrorResponse handleFailure(Exception exception) {
        HttpStatus status = exception instanceof IllegalArgumentException
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.CONFLICT;

        return ApiErrorResponse.of(status, exception.getMessage());
    }

    private void publish(Principal principal, NotificationSocketEvent event) {
        // principal.getName() is the same key the broker used to bind this
        // Principal's subscription, so this cannot address the wrong user.
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                NotificationSocketEvent.DESTINATION,
                event
        );
    }
}
