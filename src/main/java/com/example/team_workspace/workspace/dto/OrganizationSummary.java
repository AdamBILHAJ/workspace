package com.example.team_workspace.workspace.dto;

import com.example.team_workspace.workspace.domain.Organization;

public record OrganizationSummary(
        Long id,
        String name,
        String slug
) {

    public static OrganizationSummary from(Organization organization) {
        return new OrganizationSummary(
                organization.getId(),
                organization.getName(),
                organization.getSlug()
        );
    }
}
