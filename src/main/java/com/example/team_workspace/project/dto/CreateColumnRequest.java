package com.example.team_workspace.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateColumnRequest(
        @NotBlank
        @Size(max = 60)
        String name
) {
}
