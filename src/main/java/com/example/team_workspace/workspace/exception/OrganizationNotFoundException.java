package com.example.team_workspace.workspace.exception;

public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException() {
        super("Organization not found");
    }
}
