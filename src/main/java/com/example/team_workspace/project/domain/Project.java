package com.example.team_workspace.project.domain;

import java.util.Locale;

import com.example.team_workspace.common.domain.BaseAuditableEntity;
import com.example.team_workspace.workspace.domain.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "projects",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_projects_workspace_key",
                columnNames = {"workspace_id", "project_key"}
        ),
        indexes = @Index(name = "idx_projects_workspace_id", columnList = "workspace_id")
)
public class Project extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "project_key", nullable = false, length = 10)
    private String key;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    protected Project() {
    }

    public Project(String name, String key, String description, Workspace workspace) {
        setName(name);
        setKey(key);
        this.description = normalize(description);
        this.workspace = workspace;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key.trim().toUpperCase(Locale.ROOT);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = normalize(description);
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
