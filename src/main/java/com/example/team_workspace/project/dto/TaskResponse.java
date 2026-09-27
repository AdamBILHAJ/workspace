package com.example.team_workspace.project.dto;

import java.time.Instant;

import com.example.team_workspace.project.domain.Task;

public record TaskResponse(
        Long id,
        Long columnId,
        String title,
        String description,
        String priority,
        Integer orderIndex,
        TaskUserSummary assignee,
        TaskUserSummary reporter,
        Instant createdAt,
        Instant updatedAt
) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getColumn().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority().name(),
                task.getOrderIndex(),
                TaskUserSummary.from(task.getAssignee()),
                TaskUserSummary.from(task.getReporter()),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
