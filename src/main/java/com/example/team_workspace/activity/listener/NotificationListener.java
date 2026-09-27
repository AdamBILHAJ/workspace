package com.example.team_workspace.activity.listener;

import com.example.team_workspace.activity.domain.Notification;
import com.example.team_workspace.activity.event.CommentAddedEvent;
import com.example.team_workspace.activity.event.TaskCreatedEvent;
import com.example.team_workspace.activity.repository.NotificationRepository;
import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.user.repository.UserRepository;
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

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationListener(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
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

        notificationRepository.save(new Notification(
                assignee,
                title,
                message,
                buildTargetUrl(workspaceSlug, projectKey, taskId)
        ));
    }

    private String buildTargetUrl(String workspaceSlug, String projectKey, Long taskId) {
        return "/workspaces/" + workspaceSlug + "/projects/" + projectKey + "?task=" + taskId;
    }
}
