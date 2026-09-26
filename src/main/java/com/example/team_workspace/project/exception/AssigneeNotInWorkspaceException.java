package com.example.team_workspace.project.exception;

public class AssigneeNotInWorkspaceException extends RuntimeException {

    public AssigneeNotInWorkspaceException() {
        super("The selected assignee is not a member of this workspace");
    }
}
