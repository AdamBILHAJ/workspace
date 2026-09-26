package com.example.team_workspace.workspace.dto;

import com.example.team_workspace.workspace.domain.Workspace;
import com.example.team_workspace.workspace.domain.WorkspaceRole;

public record WorkspaceResponse(
        Long id,
        String name,
        String slug,
        OrganizationSummary organization,
        WorkspaceRole role
) {

    public static WorkspaceResponse from(Workspace workspace, WorkspaceRole role) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                OrganizationSummary.from(workspace.getOrganization()),
                role
        );
    }
}
