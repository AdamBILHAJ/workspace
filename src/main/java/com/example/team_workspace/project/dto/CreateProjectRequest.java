package com.example.team_workspace.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank
        @Size(max = 120)
        String name,
        @Pattern(regexp = "[A-Za-z0-9]{2,10}")
        String key,
        @Size(max = 2000)
        String description
) {
}
