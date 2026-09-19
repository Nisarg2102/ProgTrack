package com.threespace.progtrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── FIX: Changed from LAZY to EAGER ─────────────────────────────────────
    // LAZY fetch causes LazyInitializationException when Thymeleaf accesses
    // leave.user.fullName outside the Hibernate session (after the service
    // method returns). This crashes the HTTP response mid-stream, producing
    // ERR_INCOMPLETE_CHUNKED_ENCODING in the browser.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveType type;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "processed_by", length = 150)
    private String processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "lead_comment", columnDefinition = "TEXT")
    private String leadComment;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum LeaveType {
        CASUAL, SICK, EARNED, MATERNITY, PATERNITY, UNPAID, COMPENSATORY
    }

    public enum LeaveStatus {
        PENDING, APPROVED, REJECTED, CANCELLED
    }

    // =====================
    // Constructors
    // =====================

    public LeaveRequest() {}

    public LeaveRequest(User user, LocalDate startDate, LocalDate endDate, LeaveType type, String reason) {
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.reason = reason;
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
    // Utility Methods
    // =====================

    /**
     * Calculate number of leave days requested.
     */
    public long getLeaveDays() {
        if (startDate == null || endDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    // =====================
    // Getters & Setters
    // =====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LeaveType getType() { return type; }
    public void setType(LeaveType type) { this.type = type; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) { this.status = status; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public String getLeadComment() { return leadComment; }
    public void setLeadComment(String leadComment) { this.leadComment = leadComment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "LeaveRequest{" +
               "id=" + id +
               ", startDate=" + startDate +
               ", endDate=" + endDate +
               ", type=" + type +
               ", status=" + status +
               ", processedBy='" + processedBy + '\'' +
               '}';
    }
}