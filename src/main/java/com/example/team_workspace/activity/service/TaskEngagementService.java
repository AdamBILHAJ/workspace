package com.example.team_workspace.activity.service;

import java.util.List;

import com.example.team_workspace.activity.domain.TaskComment;
import com.example.team_workspace.activity.dto.ActivityLogResponse;
import com.example.team_workspace.activity.dto.CommentResponse;
import com.example.team_workspace.activity.dto.CreateCommentRequest;
import com.example.team_workspace.activity.event.CommentAddedEvent;
import com.example.team_workspace.activity.repository.ActivityLogRepository;
import com.example.team_workspace.activity.repository.TaskCommentRepository;
import com.example.team_workspace.project.domain.Task;
import com.example.team_workspace.project.exception.TaskNotFoundException;
import com.example.team_workspace.project.repository.TaskRepository;
import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.workspace.service.MembershipGuard;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskEngagementService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final MembershipGuard membershipGuard;
    private final ApplicationEventPublisher eventPublisher;

    public TaskEngagementService(
            TaskRepository taskRepository,
            TaskCommentRepository taskCommentRepository,
            ActivityLogRepository activityLogRepository,
            MembershipGuard membershipGuard,
            ApplicationEventPublisher eventPublisher
    ) {
        this.taskRepository = taskRepository;
        this.taskCommentRepository = taskCommentRepository;
        this.activityLogRepository = activityLogRepository;
        this.membershipGuard = membershipGuard;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CommentResponse addComment(Long taskId, CreateCommentRequest request, User actor) {
        Task task = requireReadableTask(taskId, actor);
        User author = membershipGuard.requireCurrentUser(actor);

        TaskComment comment = taskCommentRepository.save(
                new TaskComment(request.content(), task, author)
        );

        eventPublisher.publishEvent(CommentAddedEvent.from(comment, task, author));

        return CommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(Long taskId, User actor) {
        requireReadableTask(taskId, actor);
        return taskCommentRepository.findAllByTaskIdOrderByCreatedAtAscIdAsc(taskId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> listActivity(Long taskId, User actor) {
        requireReadableTask(taskId, actor);
        return activityLogRepository.findAllByTaskIdOrderByCreatedAtDescIdDesc(taskId)
                .stream()
                .map(ActivityLogResponse::from)
                .toList();
    }

    /**
     * Resolves the task and confirms the caller still belongs to the workspace
     * that owns it, so comments and activity are never readable across tenants.
     */
    private Task requireReadableTask(Long taskId, User actor) {
        Task task = taskRepository.findByIdWithUsers(taskId)
                .orElseThrow(TaskNotFoundException::new);
        membershipGuard.requireMembership(
                task.getColumn().getProject().getWorkspace().getId(),
                actor
        );
        return task;
    }
}
