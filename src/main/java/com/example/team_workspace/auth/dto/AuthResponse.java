package com.example.team_workspace.auth.dto;

import com.example.team_workspace.user.domain.User;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserSummary userSummary
) {

    public static AuthResponse from(User user, String accessToken) {
        return new AuthResponse(accessToken, "Bearer", UserSummary.from(user));
    }
}
