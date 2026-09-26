package com.example.team_workspace.project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveTaskRequest(
        @NotNull
        Long columnId,
        @NotNull
        @Min(0)
        Integer orderIndex
) {
}
