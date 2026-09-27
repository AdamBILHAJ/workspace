package com.example.team_workspace.project.dto;

import com.example.team_workspace.project.domain.Project;

public record ProjectSummary(
        Long id,
        String name,
        String key,
        String description,
        Long workspaceId,
        String workspaceName,
        String workspaceSlug
) {

    public static ProjectSummary from(Project project) {
        return new ProjectSummary(
                project.getId(),
                project.getName(),
                project.getKey(),
                project.getDescription(),
                project.getWorkspace().getId(),
                project.getWorkspace().getName(),
                project.getWorkspace().getSlug()
        );
    }
}
