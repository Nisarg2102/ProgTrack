package com.threespace.progtrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Notification message is required")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_seen")
    private Boolean isSeen = false;

    @Column(name = "action_url", length = 512)
    private String actionUrl;

    @Column(name = "seen_at")
    private LocalDateTime seenAt;

    public enum NotificationType {
        LEAVE_APPROVED, LEAVE_REJECTED, LEAVE_REQUESTED,
        TASK_REMINDER, TASK_SUBMITTED, TASK_REVIEWED,
        CHAT_MESSAGE, SYSTEM, ANNOUNCEMENT,
        REPORT_SUBMITTED,
        REPORT_REVIEWED,
        REPORT_DEADLINE_REMINDER,
        SYSTEM_ALERT
    }

    // =====================
    // Constructors
    // =====================

    public Notification() {
        this.createdAt = LocalDateTime.now();
    }

    public Notification(User user, String message, NotificationType type) {
        this.user = user;
        this.message = message;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.isSeen = false;
    }

    public Notification(User user, String message, NotificationType type, String actionUrl) {
        this.user = user;
        this.message = message;
        this.type = type;
        this.actionUrl = actionUrl;
        this.createdAt = LocalDateTime.now();
        this.isSeen = false;
    }

    // =====================
    // Getters & Setters
    // =====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsSeen() { return isSeen; }
    public void setIsSeen(Boolean isSeen) { this.isSeen = isSeen; }

    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

    public LocalDateTime getSeenAt() { return seenAt; }
    public void setSeenAt(LocalDateTime seenAt) { this.seenAt = seenAt; }

    // =====================
    // toString
    // =====================

    @Override
    public String toString() {
        return "Notification{" +
               "id=" + id +
               ", message='" + message + '\'' +
               ", type=" + type +
               ", isSeen=" + isSeen +
               ", createdAt=" + createdAt +
               '}';
    }
}
