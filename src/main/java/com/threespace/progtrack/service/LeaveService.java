package com.threespace.progtrack.service;

import com.threespace.progtrack.entity.LeaveRequest;
import com.threespace.progtrack.entity.Notification;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.LeaveRequestRepository;
import com.threespace.progtrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    // =====================
    // APPLY
    // =====================

    public LeaveRequest applyForLeave(User user, LeaveRequest leaveRequest) {
        leaveRequest.setUser(user);
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.PENDING);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("📅 Leave applied by: {} ({} to {})", user.getEmail(), saved.getStartDate(), saved.getEndDate());

        // FIX: Route Notifications based on Hierarchy
        List<User> approvers = new ArrayList<>();
        
        if (user.getRole() == User.Role.INTERN) {
            // Interns notify Leads in their department
            approvers = userRepository.findActiveUsersByDepartment(user.getDepartment())
                .stream()
                .filter(u -> User.Role.LEAD.equals(u.getRole()))
                .collect(Collectors.toList());
        } else if (user.getRole() == User.Role.LEAD) {
            // Leads notify the CEO (Iterating all to find CEO in case CEO is in a 'Corporate' dept)
            approvers = userRepository.findAll()
                .stream()
                .filter(u -> User.Role.CEO.equals(u.getRole()))
                .collect(Collectors.toList());
        }

        approvers.forEach(approver -> notificationService.createNotification(
            approver,
            user.getFullName() + " has applied for " + leaveRequest.getType().name() + " leave ("
                + leaveRequest.getLeaveDays() + " day(s)).",
            Notification.NotificationType.LEAVE_REQUESTED,
            "/leaves/manage?highlight=" + saved.getId()
        ));

        return saved;
    }

    // =====================
    // READ
    // =====================

    @Transactional(readOnly = true)
    public List<LeaveRequest> getLeaveRequestsForUser(User user) {
        return leaveRequestRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getDepartmentLeaveRequests(String department) {
        return leaveRequestRepository.findByDepartment(department);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getPendingDepartmentLeaves(String department) {
        return leaveRequestRepository.findByDepartmentAndStatus(
            department, LeaveRequest.LeaveStatus.PENDING
        );
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepository.findAllOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public LeaveRequest getById(Long id) {
        return leaveRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Leave request not found: " + id));
    }

    // =====================
    // APPROVE / REJECT
    // =====================

    public LeaveRequest approveLeave(Long leaveId, String approverEmail, String comment) {
        LeaveRequest leave = getById(leaveId);

        if (!LeaveRequest.LeaveStatus.PENDING.equals(leave.getStatus())) {
            throw new IllegalStateException("Only pending leave requests can be approved.");
        }

        leave.setStatus(LeaveRequest.LeaveStatus.APPROVED);
        leave.setProcessedBy(approverEmail);
        leave.setProcessedAt(LocalDateTime.now());
        leave.setLeadComment(comment);

        LeaveRequest updated = leaveRequestRepository.save(leave);

        notificationService.createNotification(
            leave.getUser(),
            "✅ Your " + leave.getType().name() + " leave request (" + leave.getLeaveDays() + " day(s)) has been APPROVED.",
            Notification.NotificationType.LEAVE_APPROVED,
            "/leaves/my?highlight=" + leave.getId()
        );

        log.info("✅ Leave approved — ID: {} by: {}", leaveId, approverEmail);
        return updated;
    }

    public LeaveRequest rejectLeave(Long leaveId, String approverEmail, String comment) {
        LeaveRequest leave = getById(leaveId);

        if (!LeaveRequest.LeaveStatus.PENDING.equals(leave.getStatus())) {
            throw new IllegalStateException("Only pending leave requests can be rejected.");
        }

        leave.setStatus(LeaveRequest.LeaveStatus.REJECTED);
        leave.setProcessedBy(approverEmail);
        leave.setProcessedAt(LocalDateTime.now());
        leave.setLeadComment(comment);

        LeaveRequest updated = leaveRequestRepository.save(leave);

        notificationService.createNotification(
            leave.getUser(),
            "❌ Your " + leave.getType().name() + " leave request (" + leave.getLeaveDays() + " day(s)) has been REJECTED. Reason: " + comment,
            Notification.NotificationType.LEAVE_REJECTED,
            "/leaves/my?highlight=" + leave.getId()
        );

        log.info("❌ Leave rejected — ID: {} by: {}", leaveId, approverEmail);
        return updated;
    }

    public LeaveRequest cancelLeave(Long leaveId, User requestingUser) {
        LeaveRequest leave = getById(leaveId);

        if (!leave.getUser().getId().equals(requestingUser.getId())) {
            throw new SecurityException("You cannot cancel someone else's leave request.");
        }
        if (!LeaveRequest.LeaveStatus.PENDING.equals(leave.getStatus())) {
            throw new IllegalStateException("Only pending leave requests can be cancelled.");
        }

        leave.setStatus(LeaveRequest.LeaveStatus.CANCELLED);
        return leaveRequestRepository.save(leave);
    }
}