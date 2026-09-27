package com.example.team_workspace.activity.domain;

import com.example.team_workspace.common.domain.BaseAuditableEntity;
import com.example.team_workspace.project.domain.Task;
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
        name = "task_comments",
        indexes = {
                @Index(name = "idx_task_comments_task_id", columnList = "task_id"),
                @Index(name = "idx_task_comments_author_id", columnList = "author_id")
        }
)
public class TaskComment extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    protected TaskComment() {
    }

    public TaskComment(String content, Task task, User author) {
        setContent(content);
        this.task = task;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
    }

    public Task getTask() {
        return task;
    }

    public User getAuthor() {
        return author;
    }
}
