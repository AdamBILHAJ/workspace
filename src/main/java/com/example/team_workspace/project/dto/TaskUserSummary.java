package com.example.team_workspace.project.dto;

import com.example.team_workspace.user.domain.User;

public record TaskUserSummary(
        Long id,
        String email,
        String firstName,
        String lastName
) {

    public static TaskUserSummary from(User user) {
        if (user == null) {
            return null;
        }
        return new TaskUserSummary(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }
}
