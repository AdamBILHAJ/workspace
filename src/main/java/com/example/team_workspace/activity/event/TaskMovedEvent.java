package com.example.team_workspace.activity.event;

import com.example.team_workspace.project.domain.Task;
import com.example.team_workspace.user.domain.User;

public record TaskMovedEvent(
        Long taskId,
        String taskTitle,
        String projectKey,
        String workspaceSlug,
        String fromColumnName,
        String toColumnName,
        Long actorId,
        Long assigneeId
) {

    public static TaskMovedEvent from(
            Task task,
            User actor,
            String fromColumnName
    ) {
        return new TaskMovedEvent(
                task.getId(),
                task.getTitle(),
                task.getColumn().getProject().getKey(),
                task.getColumn().getProject().getWorkspace().getSlug(),
                fromColumnName,
                task.getColumn().getName(),
                actor.getId(),
                task.getAssignee() == null ? null : task.getAssignee().getId()
        );
    }
}
