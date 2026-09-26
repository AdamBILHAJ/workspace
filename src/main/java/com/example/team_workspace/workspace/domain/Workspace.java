package com.example.team_workspace.workspace.domain;

import com.example.team_workspace.common.domain.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "workspaces",
        indexes = {
                @Index(name = "idx_workspaces_slug", columnList = "slug", unique = true),
                @Index(name = "idx_workspaces_organization_id", columnList = "organization_id")
        }
)
public class Workspace extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    protected Workspace() {
    }

    public Workspace(String name, String slug, Organization organization) {
        setName(name);
        this.slug = slug;
        this.organization = organization;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getSlug() {
        return slug;
    }

    public Organization getOrganization() {
        return organization;
    }
}
