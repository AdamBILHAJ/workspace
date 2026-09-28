package com.example.team_workspace.activity.listener;

import com.example.team_workspace.activity.domain.Notification;
import com.example.team_workspace.activity.dto.NotificationResponse;
import com.example.team_workspace.activity.event.CommentAddedEvent;
import com.example.team_workspace.activity.event.TaskCreatedEvent;
import com.example.team_workspace.activity.repository.NotificationRepository;
import com.example.team_workspace.activity.socket.NotificationSocketEvent;
import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns task assignment and comments into in-app notifications. The actor is
 * never notified about their own action, which keeps the bell quiet while you
 * are working inside a task.
 */
@Component
public class NotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationListener(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTaskCreated(TaskCreatedEvent event) {
        notifyAssignee(
                event.assigneeId(),
                event.actorId(),
                "New task assigned to you",
                event.taskTitle() + " was assigned to you.",
                event.workspaceSlug(),
                event.projectKey(),
                event.taskId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommentAdded(CommentAddedEvent event) {
        notifyAssignee(
                event.assigneeId(),
                event.authorId(),
                "New comment on your task",
                event.authorName() + " commented on " + event.taskTitle() + ".",
                event.workspaceSlug(),
                event.projectKey(),
                event.taskId()
        );
    }

    private void notifyAssignee(
            Long assigneeId,
            Long actorId,
            String title,
            String message,
            String workspaceSlug,
            String projectKey,
            Long taskId
    ) {
        if (assigneeId == null || assigneeId.equals(actorId)) {
            return;
        }

        User assignee = userRepository.findById(assigneeId).orElse(null);

        if (assignee == null) {
            return;
        }

        String targetUrl = buildTargetUrl(workspaceSlug, projectKey, taskId);
        Notification saved = notificationRepository.save(new Notification(
                assignee,
                title,
                message,
                targetUrl
        ));

        pushToAssignee(assignee, saved);
    }

    /**
     * Pushes the committed notification to every live session of the assignee.
     *
     * <p>The already-persisted entity is mapped rather than a freshly built one,
     * so the payload carries a real id and createdAt and the client can dedupe
     * against the row it already loaded over REST. A delivery failure is logged
     * but never propagated: the row is already committed, and the client refetches
     * on next mount.
     */
    private void pushToAssignee(User assignee, Notification notification) {
        try {
            // Identity generation inserts on persist, so this count already
            // includes the notification being pushed.
            long unread = notificationRepository.countByRecipientIdAndIsReadFalse(assignee.getId());

            messagingTemplate.convertAndSendToUser(
                    // Principal.getName() for a wrapped User is the email, which
                    // is the same key used for /user/** subscription routing.
                    assignee.getEmail(),
                    NotificationSocketEvent.DESTINATION,
                    NotificationSocketEvent.newNotification(
                            NotificationResponse.from(notification),
                            unread
                    )
            );
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Stored notification for user {} but could not push it: {}",
                    assignee.getEmail(),
                    exception.getMessage()
            );
        }
    }

    private String buildTargetUrl(String workspaceSlug, String projectKey, Long taskId) {
        return "/workspaces/" + workspaceSlug + "/projects/" + projectKey + "?task=" + taskId;
    }
}
