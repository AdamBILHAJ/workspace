package com.example.team_workspace.project.dto;

import java.util.List;

import com.example.team_workspace.project.domain.KanbanColumn;

public record BoardColumnResponse(
        Long id,
        String name,
        Integer orderIndex,
        List<TaskResponse> tasks
) {

    public static BoardColumnResponse from(KanbanColumn column, List<TaskResponse> tasks) {
        return new BoardColumnResponse(
                column.getId(),
                column.getName(),
                column.getOrderIndex(),
                List.copyOf(tasks)
        );
    }
}
