package com.example.team_workspace.activity.event;

import com.example.team_workspace.project.domain.Task;
import com.example.team_workspace.user.domain.User;

/**
 * Published after a task is created so listeners can record activity and
 * notify the assignee without TaskService depending on them.
 */
public record TaskCreatedEvent(
        Long taskId,
        String taskTitle,
        String projectKey,
        String workspaceSlug,
        String columnName,
        Long actorId,
        Long assigneeId
) {

    public static TaskCreatedEvent from(Task task, User actor) {
        return new TaskCreatedEvent(
                task.getId(),
                task.getTitle(),
                task.getColumn().getProject().getKey(),
                task.getColumn().getProject().getWorkspace().getSlug(),
                task.getColumn().getName(),
                actor.getId(),
                task.getAssignee() == null ? null : task.getAssignee().getId()
        );
    }
}
