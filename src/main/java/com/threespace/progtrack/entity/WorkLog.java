package com.threespace.progtrack.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_logs")
public class WorkLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "login_date_time", nullable = false)
    private LocalDateTime loginDateTime;

    @Column(name = "logout_date_time")
    private LocalDateTime logoutDateTime;

    @Column(name = "total_seconds")
    private Long totalSeconds;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // =====================
    // Constructors
    // =====================

    public WorkLog() {}

    public WorkLog(User user, LocalDateTime loginDateTime, String sessionId) {
        this.user = user;
        this.loginDateTime = loginDateTime;
        this.sessionId = sessionId;
    }

    public WorkLog(User user, LocalDateTime loginDateTime, LocalDateTime logoutDateTime, Long totalSeconds) {
        this.user = user;
        this.loginDateTime = loginDateTime;
        this.logoutDateTime = logoutDateTime;
        this.totalSeconds = totalSeconds;
    }

    // =====================
    // Lifecycle Callbacks
    // =====================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // =====================
    // Utility Methods
    // =====================

    /**
     * Calculate total seconds between login and logout.
     * Automatically sets totalSeconds field.
     */
    public void calculateTotalSeconds() {
        if (this.loginDateTime != null && this.logoutDateTime != null) {
            this.totalSeconds = java.time.Duration.between(this.loginDateTime, this.logoutDateTime).getSeconds();
        }
    }

    /**
     * Returns formatted duration string (e.g. "7h 23m 10s")
     */
    public String getFormattedDuration() {
        if (this.totalSeconds == null || this.totalSeconds <= 0) {
            return "0h 0m 0s";
        }
        long hours = this.totalSeconds / 3600;
        long minutes = (this.totalSeconds % 3600) / 60;
        long seconds = this.totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    /**
     * Returns total hours as a decimal (e.g. 7.5 for 7h 30m)
     */
    public Double getTotalHours() {
        if (this.totalSeconds == null) return 0.0;
        return this.totalSeconds / 3600.0;
    }

    // =====================
    // Getters & Setters
    // =====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getLoginDateTime() { return loginDateTime; }
    public void setLoginDateTime(LocalDateTime loginDateTime) { this.loginDateTime = loginDateTime; }

    public LocalDateTime getLogoutDateTime() { return logoutDateTime; }
    public void setLogoutDateTime(LocalDateTime logoutDateTime) { this.logoutDateTime = logoutDateTime; }

    public Long getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(Long totalSeconds) { this.totalSeconds = totalSeconds; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // =====================
    // toString
    // =====================

    @Override
    public String toString() {
        return "WorkLog{" +
               "id=" + id +
               ", loginDateTime=" + loginDateTime +
               ", logoutDateTime=" + logoutDateTime +
               ", totalSeconds=" + totalSeconds +
               ", sessionId='" + sessionId + '\'' +
               '}';
    }
}
