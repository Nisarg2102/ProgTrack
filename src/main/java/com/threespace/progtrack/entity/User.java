package com.threespace.progtrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 150)
    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    private String password;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Column(length = 100)
    private String designation;

    @Column(length = 100)
    private String department;

    @Column(name = "project_group", length = 100)
    private String projectGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
 // ADD THIS NEW FIELD
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    @Column(name = "current_login_time")
    private LocalDateTime currentLoginTime;

    @Column(name = "profile_picture", length = 255)
    private String profilePicture;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DailyTask> dailyTasks = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkLog> workLogs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LeaveRequest> leaveRequests = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();

    public enum Role {
        ADMIN, CEO, LEAD, INTERN
    }

    // =====================
    // Constructors
    // =====================

    public User() {}

    public User(String email, String password, String fullName, Role role) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public User(Long id, String email, String password, String fullName, String designation,
                String department, String projectGroup, Role role, Boolean isActive,
                LocalDateTime currentLoginTime) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.designation = designation;
        this.department = department;
        this.projectGroup = projectGroup;
        this.role = role;
        this.isActive = isActive;
        this.currentLoginTime = currentLoginTime;
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

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getProjectGroup() { return projectGroup; }
    public void setProjectGroup(String projectGroup) { this.projectGroup = projectGroup; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }

    public LocalDateTime getCurrentLoginTime() { return currentLoginTime; }
    public void setCurrentLoginTime(LocalDateTime currentLoginTime) { this.currentLoginTime = currentLoginTime; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<DailyTask> getDailyTasks() { return dailyTasks; }
    public void setDailyTasks(List<DailyTask> dailyTasks) { this.dailyTasks = dailyTasks; }

    public List<WorkLog> getWorkLogs() { return workLogs; }
    public void setWorkLogs(List<WorkLog> workLogs) { this.workLogs = workLogs; }

    public List<LeaveRequest> getLeaveRequests() { return leaveRequests; }
    public void setLeaveRequests(List<LeaveRequest> leaveRequests) { this.leaveRequests = leaveRequests; }

    public List<Notification> getNotifications() { return notifications; }
    public void setNotifications(List<Notification> notifications) { this.notifications = notifications; }

    // =====================
    // toString
    // =====================

    @Override
    public String toString() {
        return "User{" +
               "id=" + id +
               ", email='" + email + '\'' +
               ", fullName='" + fullName + '\'' +
               ", designation='" + designation + '\'' +
               ", department='" + department + '\'' +
               ", role=" + role +
               ", isActive=" + isActive +
               '}';
    }
}
