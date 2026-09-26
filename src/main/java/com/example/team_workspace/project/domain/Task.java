package com.example.team_workspace.project.domain;

import com.example.team_workspace.common.domain.BaseAuditableEntity;
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
        name = "tasks",
        indexes = {
                @Index(name = "idx_tasks_column_order", columnList = "column_id, order_index"),
                @Index(name = "idx_tasks_assignee_id", columnList = "assignee_id")
        }
)
public class Task extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "column_id", nullable = false)
    private KanbanColumn column;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    protected Task() {
    }

    public Task(
            String title,
            String description,
            TaskPriority priority,
            Integer orderIndex,
            KanbanColumn column,
            User assignee,
            User reporter
    ) {
        setTitle(title);
        setDescription(description);
        this.priority = priority;
        this.orderIndex = orderIndex;
        this.column = column;
        this.assignee = assignee;
        this.reporter = reporter;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description == null || description.isBlank()) {
            this.description = null;
            return;
        }
        this.description = description.trim();
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public KanbanColumn getColumn() {
        return column;
    }

    public void setColumn(KanbanColumn column) {
        this.column = column;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public User getReporter() {
        return reporter;
    }
}
