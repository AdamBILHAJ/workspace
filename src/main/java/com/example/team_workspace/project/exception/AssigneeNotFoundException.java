package com.example.team_workspace.project.exception;

public class AssigneeNotFoundException extends RuntimeException {

    public AssigneeNotFoundException() {
        super("The selected assignee does not exist");
    }
}
