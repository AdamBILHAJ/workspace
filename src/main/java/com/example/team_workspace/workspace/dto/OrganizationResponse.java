package com.example.team_workspace.workspace.dto;

import com.example.team_workspace.workspace.domain.Organization;

public record OrganizationResponse(
        Long id,
        String name,
        String slug
) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug()
        );
    }
}
