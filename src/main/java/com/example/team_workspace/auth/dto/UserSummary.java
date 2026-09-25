package com.example.team_workspace.auth.dto;

import com.example.team_workspace.user.domain.Role;
import com.example.team_workspace.user.domain.User;

public record UserSummary(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role
) {

    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}
