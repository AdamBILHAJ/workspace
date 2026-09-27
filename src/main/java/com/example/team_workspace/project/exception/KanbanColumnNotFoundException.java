package com.example.team_workspace.project.exception;

public class KanbanColumnNotFoundException extends RuntimeException {

    public KanbanColumnNotFoundException() {
        super("Column not found");
    }
}
