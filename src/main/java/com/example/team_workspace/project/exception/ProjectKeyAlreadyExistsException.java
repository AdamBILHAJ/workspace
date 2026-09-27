package com.example.team_workspace.project.exception;

public class ProjectKeyAlreadyExistsException extends RuntimeException {

    public ProjectKeyAlreadyExistsException() {
        super("A project with this key already exists in the workspace");
    }
}
