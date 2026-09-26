package com.example.team_workspace.workspace.domain;

import com.example.team_workspace.common.domain.BaseAuditableEntity;
import com.example.team_workspace.user.domain.User;
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
        name = "organizations",
        indexes = {
                @Index(name = "idx_organizations_slug", columnList = "slug", unique = true),
                @Index(name = "idx_organizations_owner_id", columnList = "owner_id")
        }
)
public class Organization extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    protected Organization() {
    }

    public Organization(String name, String slug, User owner) {
        setName(name);
        this.slug = slug;
        this.owner = owner;
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

    public User getOwner() {
        return owner;
    }
}
