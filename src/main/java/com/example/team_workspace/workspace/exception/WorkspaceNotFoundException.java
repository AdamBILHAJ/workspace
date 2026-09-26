package com.example.team_workspace.workspace.exception;

public class WorkspaceNotFoundException extends RuntimeException {

    public WorkspaceNotFoundException() {
        super("Workspace not found");
    }
}
