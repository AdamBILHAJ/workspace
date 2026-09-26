package com.example.team_workspace.project.domain;

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
        name = "kanban_columns",
        indexes = @Index(
                name = "idx_kanban_columns_project_order",
                columnList = "project_id, order_index"
        )
)
public class KanbanColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    protected KanbanColumn() {
    }

    public KanbanColumn(String name, Integer orderIndex, Project project) {
        setName(name);
        this.orderIndex = orderIndex;
        this.project = project;
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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Project getProject() {
        return project;
    }
}
