package com.example.team_workspace.activity.domain;

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
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id, is_read"),
                @Index(name = "idx_notifications_recipient_id", columnList = "recipient_id")
        }
)
public class Notification extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    protected Notification() {
    }

    public Notification(User recipient, String title, String message, String targetUrl) {
        this.recipient = recipient;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
    }

    public Long getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public String getTargetUrl() {
        return targetUrl;
    }
}
