package com.example.team_workspace.workspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddMemberRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        String email
) {
}
