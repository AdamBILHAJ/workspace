package com.example.team_workspace.project.dto;

import java.time.Instant;

import com.example.team_workspace.project.domain.Project;

public record ProjectResponse(
        Long id,
        String name,
        String key,
        String description,
        Long workspaceId,
        String workspaceName,
        String workspaceSlug,
        int totalTasks,
        int completedTasks,
        int completionPercent,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProjectResponse from(Project project, int totalTasks, int completedTasks) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getKey(),
                project.getDescription(),
                project.getWorkspace().getId(),
                project.getWorkspace().getName(),
                project.getWorkspace().getSlug(),
                totalTasks,
                completedTasks,
                completionPercent(totalTasks, completedTasks),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private static int completionPercent(int totalTasks, int completedTasks) {
        if (totalTasks <= 0) {
            return 0;
        }
        return (int) Math.round((completedTasks * 100.0) / totalTasks);
    }
}
