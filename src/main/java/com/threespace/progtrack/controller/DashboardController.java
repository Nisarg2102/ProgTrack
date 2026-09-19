package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired private UserService userService;
    @Autowired private TaskService taskService;
    @Autowired private LeaveService leaveService;
    @Autowired private NotificationService notificationService;
    @Autowired private ReportSubmissionService reportSubmissionService;

    // ─── Root redirect ────────────────────────────────────────────────────────
    @GetMapping({"/", "/home", "/dashboard"})
    public String rootRedirect(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getByEmail(userDetails.getUsername());
        return switch (user.getRole()) {
            case ADMIN  -> "redirect:/admin/dashboard";
            case CEO    -> "redirect:/dashboard/executive";
            case LEAD -> "redirect:/dashboard/lead";
            case INTERN -> "redirect:/dashboard/intern";
        };
    }

    // ─── Intern Dashboard ─────────────────────────────────────────────────────
    @GetMapping("/dashboard/intern")
    @Transactional(readOnly = true)
    public String internDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getByEmail(userDetails.getUsername());

        Map<String, Object> stats = taskService.getInternDashboardStats(user);
        long elapsedSeconds = taskService.getCurrentSessionElapsedSeconds(user);

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("elapsedSeconds", elapsedSeconds);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("notifications", notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(user));
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("taskStatuses", com.threespace.progtrack.entity.DailyTask.TaskStatus.values());
        model.addAttribute("leaveTypes", com.threespace.progtrack.entity.LeaveRequest.LeaveType.values());

        return "dashboard/intern";
    }

    // ─── Lead Dashboard ────────────────────────────────────────────────────
    // FIX: @Transactional(readOnly = true) keeps the Hibernate session open
    // for the entire request — including Thymeleaf template rendering.
    // Without this, any LAZY-fetched association (like leave.user or
    // member.someRelation) triggers LazyInitializationException mid-render,
    // which abruptly closes the HTTP response and produces
    // ERR_INCOMPLETE_CHUNKED_ENCODING in the browser.
    @GetMapping("/dashboard/lead")
    @Transactional(readOnly = true)
    public String leadDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getByEmail(userDetails.getUsername());

        Map<String, Object> stats = taskService.getLeadDashboardStats(user);
        List<com.threespace.progtrack.entity.LeaveRequest> pendingLeaves =
            leaveService.getPendingDepartmentLeaves(user.getDepartment());
        
     // ==========================================
        // NEW: Calculate Pending Weekly & Monthly Reviews
        // ==========================================
        long pendingWeekly = reportSubmissionService.getReportsByDepartmentAndType(user.getDepartment(), com.threespace.progtrack.entity.ReportSubmission.ReportType.WEEKLY)
            .stream().filter(r -> r.getStatus() == com.threespace.progtrack.entity.ReportSubmission.ReportStatus.PENDING).count();
            
        long pendingMonthly = reportSubmissionService.getReportsByDepartmentAndType(user.getDepartment(), com.threespace.progtrack.entity.ReportSubmission.ReportType.MONTHLY)
            .stream().filter(r -> r.getStatus() == com.threespace.progtrack.entity.ReportSubmission.ReportStatus.PENDING).count();
            
        stats.put("pendingReviews", pendingWeekly + pendingMonthly);

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("pendingLeaves", pendingLeaves);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("notifications", notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(user));
        model.addAttribute("pageTitle", "Lead Dashboard — " + user.getDepartment());

        return "dashboard/lead";
    }

    // ─── CEO/Executive Dashboard ──────────────────────────────────────────────
 // ─── CEO/Executive Dashboard ──────────────────────────────────────────────
    @GetMapping("/dashboard/executive")
    @Transactional(readOnly = true)
    public String executiveDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getByEmail(userDetails.getUsername());

        Map<String, Object> stats = taskService.getExecutiveDashboardStats();
        List<String> departments = userService.getAllDepartments();

        // 🟢 FIX: Fetch pending leaves and pass them to the CEO dashboard
        // The HTML template is already set up to filter this list to only show LEAD leaves!
        List<com.threespace.progtrack.entity.LeaveRequest> pendingLeaves = leaveService.getAllLeaveRequests()
                .stream()
                .filter(l -> com.threespace.progtrack.entity.LeaveRequest.LeaveStatus.PENDING.equals(l.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("departments", departments);
        model.addAttribute("pendingLeaves", pendingLeaves); // Added here!
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("notifications", notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(user));
        model.addAttribute("pageTitle", "Executive Dashboard");

        return "dashboard/executive";
    }
   
    
}