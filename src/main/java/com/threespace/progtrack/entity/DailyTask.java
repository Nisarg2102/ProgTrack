package com.threespace.progtrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_tasks")
public class DailyTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "task_date", nullable = false)
    @NotNull(message = "Task date is required")
    private LocalDate taskDate;

    @Column(name = "task_description", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Task description is required")
    private String taskDescription;

    @Column(name = "time_spent")
    private Double timeSpent; // in hours

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.IN_PROGRESS;

    @Column(name = "google_doc_link", length = 512)
    private String googleDocLink;

    @Column(name = "task_category", length = 100)
    private String taskCategory;

    @Column(name = "priority", length = 20)
    private String priority = "MEDIUM";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "submitted")
    private Boolean submitted = false;

    public enum TaskStatus {
        IN_PROGRESS, COMPLETED, PENDING, BLOCKED, REVIEW
    }

    // =====================
    // Constructors
    // =====================

    public DailyTask() {}

    public DailyTask(User user, LocalDate taskDate, String taskDescription, Double timeSpent,
                     TaskStatus status, String googleDocLink) {
        this.user = user;
        this.taskDate = taskDate;
        this.taskDescription = taskDescription;
        this.timeSpent = timeSpent;
        this.status = status;
        this.googleDocLink = googleDocLink;
    }

    // =====================
    // Lifecycle Callbacks
    // =====================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // =====================
    // Getters & Setters
    // =====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getTaskDate() { return taskDate; }
    public void setTaskDate(LocalDate taskDate) { this.taskDate = taskDate; }

    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }

    public Double getTimeSpent() { return timeSpent; }
    public void setTimeSpent(Double timeSpent) { this.timeSpent = timeSpent; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public String getGoogleDocLink() { return googleDocLink; }
    public void setGoogleDocLink(String googleDocLink) { this.googleDocLink = googleDocLink; }

    public String getTaskCategory() { return taskCategory; }
    public void setTaskCategory(String taskCategory) { this.taskCategory = taskCategory; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getSubmitted() { return submitted; }
    public void setSubmitted(Boolean submitted) { this.submitted = submitted; }

    // =====================
    // toString
    // =====================

    @Override
    public String toString() {
        return "DailyTask{" +
               "id=" + id +
               ", taskDate=" + taskDate +
               ", taskDescription='" + taskDescription + '\'' +
               ", timeSpent=" + timeSpent +
               ", status=" + status +
               ", submitted=" + submitted +
               '}';
    }
}
