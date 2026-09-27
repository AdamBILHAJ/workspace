package com.example.team_workspace.project.dto;

import java.util.List;

public record BoardResponse(
        ProjectSummary project,
        List<BoardColumnResponse> columns
) {

    public BoardResponse {
        columns = List.copyOf(columns);
    }
}
