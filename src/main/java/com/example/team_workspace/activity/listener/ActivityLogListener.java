package com.example.team_workspace.activity.listener;

import com.example.team_workspace.activity.domain.ActivityAction;
import com.example.team_workspace.activity.domain.ActivityLog;
import com.example.team_workspace.activity.event.CommentAddedEvent;
import com.example.team_workspace.activity.event.TaskCreatedEvent;
import com.example.team_workspace.activity.event.TaskMovedEvent;
import com.example.team_workspace.activity.repository.ActivityLogRepository;
import com.example.team_workspace.project.domain.Task;
import com.example.team_workspace.project.exception.TaskNotFoundException;
import com.example.team_workspace.project.repository.TaskRepository;
import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.user.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Writes the audit trail. Listeners run after the originating transaction
 * commits so a rolled back task move or comment never leaves a phantom entry.
 */
@Component
public class ActivityLogListener {

    private final ActivityLogRepository activityLogRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public ActivityLogListener(
            ActivityLogRepository activityLogRepository,
            TaskRepository taskRepository,
            UserRepository userRepository
    ) {
        this.activityLogRepository = activityLogRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTaskCreated(TaskCreatedEvent event) {
        Task task = requireTask(event.taskId());
        record(ActivityAction.TASK_CREATED, "Created in " + event.columnName(), task, event.actorId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTaskMoved(TaskMovedEvent event) {
        Task task = requireTask(event.taskId());
        record(
                ActivityAction.TASK_MOVED,
                "Moved from " + event.fromColumnName() + " to " + event.toColumnName(),
                task,
                event.actorId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommentAdded(CommentAddedEvent event) {
        Task task = requireTask(event.taskId());
        record(ActivityAction.COMMENT_ADDED, "Added a comment", task, event.authorId());
    }

    private void record(ActivityAction action, String details, Task task, Long actorId) {
        User actor = userRepository.findById(actorId).orElse(null);

        if (actor == null) {
            return;
        }

        activityLogRepository.save(new ActivityLog(action, details, task, actor));
    }

    private Task requireTask(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(TaskNotFoundException::new);
    }
}
