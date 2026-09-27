package com.example.team_workspace.activity.domain;

import com.example.team_workspace.common.domain.BaseAuditableEntity;
import com.example.team_workspace.project.domain.Task;
import com.example.team_workspace.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "activity_logs",
        indexes = {
                @Index(name = "idx_activity_logs_task_id", columnList = "task_id"),
                @Index(name = "idx_activity_logs_actor_id", columnList = "actor_id")
        }
)
public class ActivityLog extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private ActivityAction action;

    @Column(length = 500)
    private String details;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    protected ActivityLog() {
    }

    public ActivityLog(ActivityAction action, String details, Task task, User actor) {
        this.action = action;
        this.details = normalize(details);
        this.task = task;
        this.actor = actor;
    }

    public Long getId() {
        return id;
    }

    public ActivityAction getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public Task getTask() {
        return task;
    }

    public User getActor() {
        return actor;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }
}
