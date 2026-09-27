package com.example.team_workspace.project.dto;

import com.example.team_workspace.project.domain.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank
        @Size(max = 200)
        String title,
        @Size(max = 10_000)
        String description,
        @NotNull
        TaskPriority priority,
        Long assigneeId
) {
}
