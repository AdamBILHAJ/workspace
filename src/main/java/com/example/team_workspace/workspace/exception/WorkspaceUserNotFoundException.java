package com.example.team_workspace.workspace.exception;

public class WorkspaceUserNotFoundException extends RuntimeException {

    public WorkspaceUserNotFoundException() {
        super("No registered user matches the provided email");
    }
}
