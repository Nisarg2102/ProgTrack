package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.DailyTask;
import com.threespace.progtrack.entity.LeaveRequest;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.entity.WorkLog;
import com.threespace.progtrack.repository.DailyTaskRepository;
import com.threespace.progtrack.repository.LeaveRequestRepository;
import com.threespace.progtrack.repository.WorkLogRepository;
import com.threespace.progtrack.service.EmailService;
import com.threespace.progtrack.service.NotificationService;
import com.threespace.progtrack.service.TaskService;
import com.threespace.progtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private TaskService taskService;
    @Autowired private NotificationService notificationService;
    @Autowired private DailyTaskRepository dailyTaskRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private WorkLogRepository workLogRepository;
    @Autowired private com.threespace.progtrack.repository.UserRepository userRepository;
    @Autowired 
    private EmailService emailService;

    // =========================================================================
    // ADMIN DASHBOARD
    // =========================================================================
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public String adminDashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User admin = userService.getByEmail(principal.getUsername());

        List<User> allUsers      = userService.getAllUsers();
        List<User> activeUsers   = userService.getAllActiveUsers();
        LocalDate today          = LocalDate.now();
        LocalDate monthStart     = today.withDayOfMonth(1);
        LocalDate weekStart      = today.with(java.time.DayOfWeek.MONDAY);

        // ── Headcount per role ──────────────────────────────────────────────
        Map<String, Long> roleCounts = new LinkedHashMap<>();
        for (User.Role role : User.Role.values()) {
            long cnt = activeUsers.stream().filter(u -> role.equals(u.getRole())).count();
            roleCounts.put(role.name(), cnt);
        }

        // ── Department breakdown ────────────────────────────────────────────
        List<String> departments = userService.getAllDepartments();
        Map<String, Long> deptUserCount = new LinkedHashMap<>();
        for (String dept : departments) {
            deptUserCount.put(dept, activeUsers.stream()
                    .filter(u -> dept.equals(u.getDepartment())).count());
        }

        // ── Task stats ──────────────────────────────────────────────────────
        List<DailyTask> monthTasks = dailyTaskRepository.findAllByDateRange(monthStart, today);
        List<DailyTask> todayTasks = dailyTaskRepository.findAllByDateRange(today, today);
        List<DailyTask> weekTasks  = dailyTaskRepository.findAllByDateRange(weekStart, today);

        long monthTaskCount  = monthTasks.size();
        long todayTaskCount  = todayTasks.size();
        long completedMonth  = monthTasks.stream().filter(t -> DailyTask.TaskStatus.COMPLETED.equals(t.getStatus())).count();
        long completionRate  = monthTaskCount > 0 ? Math.round(completedMonth * 100.0 / monthTaskCount) : 0;
        double monthHoursLogged = monthTasks.stream().mapToDouble(t -> t.getTimeSpent() != null ? t.getTimeSpent() : 0).sum();

        // Task status breakdown for chart
        Map<String, Long> taskStatusBreakdown = new LinkedHashMap<>();
        for (DailyTask.TaskStatus s : DailyTask.TaskStatus.values()) {
            taskStatusBreakdown.put(s.name().replace("_", " "),
                    monthTasks.stream().filter(t -> s.equals(t.getStatus())).count());
        }

        // Daily task trend (last 14 days)
        Map<String, Long> dailyTaskTrend = new TreeMap<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long cnt = dailyTaskRepository.findAllByDateRange(d, d).size();
            dailyTaskTrend.put(d.toString(), cnt);
        }

        // Top task submitters this month
        Map<String, Long> topSubmitters = monthTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getUser().getFullName(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        // ── Leave stats ──────────────────────────────────────────────────────
        List<LeaveRequest> allLeaves    = leaveRequestRepository.findAll();
        List<LeaveRequest> pendingLeaves = leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.PENDING);
        long approvedLeaves  = allLeaves.stream().filter(l -> LeaveRequest.LeaveStatus.APPROVED.equals(l.getStatus())).count();
        long rejectedLeaves  = allLeaves.stream().filter(l -> LeaveRequest.LeaveStatus.REJECTED.equals(l.getStatus())).count();

        Map<String, Long> leaveTypeBreakdown = new LinkedHashMap<>();
        for (LeaveRequest.LeaveType lt : LeaveRequest.LeaveType.values()) {
            long cnt = allLeaves.stream().filter(l -> lt.equals(l.getType())).count();
            if (cnt > 0) leaveTypeBreakdown.put(lt.name().replace("_", " "), cnt);
        }

        // ── Recent activity (last 10 users created) ──────────────────────────
        List<User> recentUsers = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && Boolean.TRUE.equals(u.getIsApproved()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .collect(Collectors.toList());

        // ── Online right now (last login within 8h and no logout) ────────────
        LocalDateTime eightHoursAgo = LocalDateTime.now().minusHours(8);
        List<User> onlineUsers = activeUsers.stream()
                .filter(u -> u.getCurrentLoginTime() != null
                        && u.getCurrentLoginTime().isAfter(eightHoursAgo))
                .sorted((a, b) -> b.getCurrentLoginTime().compareTo(a.getCurrentLoginTime()))
                .collect(Collectors.toList());

        // ── System stats ─────────────────────────────────────────────────────
        long totalUsers    = allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsApproved())).count();
        long activeCount   = activeUsers.size();
        long inactiveCount = totalUsers - activeCount;
        
        long pendingUsersCount = allUsers.stream().filter(u -> Boolean.FALSE.equals(u.getIsApproved()) && Boolean.TRUE.equals(u.getIsActive())).count();

        // ── Populate model ───────────────────────────────────────────────────
        model.addAttribute("user",               admin);
        model.addAttribute("allUsers",           allUsers);
        model.addAttribute("activeUsers",        activeUsers);
        model.addAttribute("onlineUsers",        onlineUsers);
        model.addAttribute("recentUsers",        recentUsers);
        model.addAttribute("pendingLeaves",      pendingLeaves);
        model.addAttribute("roleCounts",         roleCounts);
        model.addAttribute("deptUserCount",      deptUserCount);
        model.addAttribute("departments",        departments);
        model.addAttribute("totalUsers",         totalUsers);
        model.addAttribute("pendingUsersCount",  pendingUsersCount);
        model.addAttribute("activeCount",        activeCount);
        model.addAttribute("inactiveCount",      inactiveCount);
        model.addAttribute("monthTaskCount",     monthTaskCount);
        model.addAttribute("todayTaskCount",     todayTaskCount);
        model.addAttribute("completionRate",     completionRate);
        model.addAttribute("monthHoursLogged",   Math.round(monthHoursLogged * 100.0) / 100.0);
        model.addAttribute("taskStatusBreakdown",taskStatusBreakdown);
        model.addAttribute("dailyTaskTrend",     dailyTaskTrend);
        model.addAttribute("topSubmitters",      topSubmitters);
        model.addAttribute("leaveStatusCounts",  Map.of("PENDING", (long) pendingLeaves.size(), "APPROVED", approvedLeaves, "REJECTED", rejectedLeaves));
        model.addAttribute("leaveTypeBreakdown", leaveTypeBreakdown);
        model.addAttribute("today",              today);
        model.addAttribute("notifications",      notificationService.getTop10(admin));
        model.addAttribute("unreadNotifCount",   notificationService.countUnseen(admin));
        model.addAttribute("pageTitle",          "Admin Dashboard");

        return "dashboard/admin";
    }

    // =========================================================================
    // USER MANAGEMENT
    // =========================================================================

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers(@AuthenticationPrincipal UserDetails principal, Model model) {
        User admin = userService.getByEmail(principal.getUsername());
        List<User> users = userService.getAllUsers();

        Map<String, Long> roleCounts = new LinkedHashMap<>();
        for (User.Role role : User.Role.values()) {
            roleCounts.put(role.name(),
                    users.stream().filter(u -> role.equals(u.getRole()) && Boolean.TRUE.equals(u.getIsActive())).count());
        }

        model.addAttribute("user",             admin);
        model.addAttribute("allUsers",         users);
        model.addAttribute("roleCounts",       roleCounts);
        model.addAttribute("totalUsers",       (long) users.size());
        model.addAttribute("notifications",    notificationService.getTop10(admin));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(admin));
        model.addAttribute("parentPage",        "Users");
        model.addAttribute("pageTitle",        "User Management");
        return "admin/users";
    }
    
 // =========================================================================
    // PENDING APPROVALS MANAGEMENT
    // =========================================================================

    // 1. Show the Pending Approvals Page
    @GetMapping("/admin/users/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public String showPendingApprovals(@AuthenticationPrincipal UserDetails principal, Model model) {
User admin = userService.getByEmail(principal.getUsername());
        
        // Fetch ALL users sorted by newest first, so we can see the history
        List<User> requestUsers = userService.getAllUsers().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();

        // Calculate just the pending count for the badge
        long pendingCount = requestUsers.stream().filter(u -> !u.getIsApproved() && u.getIsActive()).count();

        model.addAttribute("user", admin);
        model.addAttribute("requestUsers", requestUsers);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("notifications", notificationService.getTop10(admin));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(admin));
        model.addAttribute("parentPage",        "Users");
        model.addAttribute("pageTitle", "Registration Requests");
        
        return "admin/approve-users";
    }

    // 2. Approve a User
    @PostMapping("/admin/users/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            User user = userService.getById(id);
            user.setIsApproved(true);
         // 🚨 FIX: Force the database to save the entire object, including the new flag
            userRepository.save(user);
            // userService.updateProfileByAdmin(user); // Or however you save user updates
            
         // 🚨 NEW: Send the Approval Email
            emailService.sendApprovalEmail(user.getEmail(), user.getFullName());
            
            ra.addFlashAttribute("successMsg", "✅ " + user.getFullName() + " has been approved. An email notification has been sent.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "❌ Failed to approve user: " + e.getMessage());
        }
        return "redirect:/admin/users/pending";
    }

    // 3. Reject a User
    @PostMapping("/admin/users/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String rejectUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
        	
        	User user = userService.getById(id);
        	
        	user.setIsActive(false); 
            userRepository.save(user);
        	
        	
            // Depending on your architecture, you might call userService.deleteUser(id) here
            // userService.deactivateUser(id); // Using deactivate as a placeholder, adjust if you want hard deletion
                    
         // 🚨 NEW: Send the Rejection Email
            emailService.sendRejectionEmail(user.getEmail(), user.getFullName());

            ra.addFlashAttribute("successMsg", "User application rejected. An email notification has been sent.");
        
            
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "❌ Failed to reject user: " + e.getMessage());
        }
        return "redirect:/admin/users/pending";
    }

    @GetMapping("/users/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUserForm(@AuthenticationPrincipal UserDetails principal, Model model) {
        User admin = userService.getByEmail(principal.getUsername());
        model.addAttribute("user",             admin);
        model.addAttribute("editUser",         null);
        model.addAttribute("roles",            User.Role.values());
        model.addAttribute("departments",      userService.getAllDepartments());
        model.addAttribute("projectGroups",    userService.getAllProjectGroups());
        model.addAttribute("notifications",    notificationService.getTop10(admin));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(admin));
        model.addAttribute("pageTitle",        "Add New User");
        return "admin/user-form";
    }

    @PostMapping("/users/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveNewUser(@ModelAttribute User newUser, RedirectAttributes ra) {
        try {
            userService.createUser(newUser);
            ra.addFlashAttribute("successMsg", "✅ User " + newUser.getFullName() + " created.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "❌ " + e.getMessage());
            return "redirect:/users/create";
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editUserForm(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails principal, Model model) {
        User admin = userService.getByEmail(principal.getUsername());
        User target = userService.getById(id);
        model.addAttribute("user",             admin);
        model.addAttribute("editUser",         target);
        model.addAttribute("roles",            User.Role.values());
        model.addAttribute("departments",      userService.getAllDepartments());
        model.addAttribute("projectGroups",    userService.getAllProjectGroups());
        model.addAttribute("notifications",    notificationService.getTop10(admin));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(admin));
        model.addAttribute("pageTitle",        "Edit User — " + target.getFullName());
        return "admin/user-form";
    }

    @PostMapping("/admin/users/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUser(@ModelAttribute User formUser, RedirectAttributes ra) {
        try {
            userService.updateProfileByAdmin(formUser);
            ra.addFlashAttribute("successMsg", "✅ User updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "❌ " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/deactivate/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
        try { userService.deactivateUser(id); ra.addFlashAttribute("successMsg", "User deactivated."); }
        catch (Exception e) { ra.addFlashAttribute("errorMsg", "❌ " + e.getMessage()); }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/activate/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        try { userService.activateUser(id); ra.addFlashAttribute("successMsg", "✅ User activated."); }
        catch (Exception e) { ra.addFlashAttribute("errorMsg", "❌ " + e.getMessage()); }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/reset-password/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String resetPassword(@PathVariable Long id, @RequestParam String newPassword, RedirectAttributes ra) {
        try { userService.resetPassword(id, newPassword); ra.addFlashAttribute("successMsg", "✅ Password reset."); }
        catch (Exception e) { ra.addFlashAttribute("errorMsg", "❌ " + e.getMessage()); }
        return "redirect:/admin/users";
    }

    // =========================================================================
    // AJAX ENDPOINTS
    // =========================================================================

    /** Live stats refresh for dashboard widgets */
    @GetMapping("/admin/api/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> liveStats() {
        List<User> active = userService.getAllActiveUsers();
        LocalDate today   = LocalDate.now();
        long todayTasks   = dailyTaskRepository.findAllByDateRange(today, today).size();
        long pending      = leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.PENDING).size();
        LocalDateTime since8h = LocalDateTime.now().minusHours(8);
        long online       = active.stream().filter(u -> u.getCurrentLoginTime() != null
                && u.getCurrentLoginTime().isAfter(since8h)).count();
        return ResponseEntity.ok(Map.of(
                "totalActive", (long) active.size(),
                "todayTasks",  todayTasks,
                "pendingLeaves", pending,
                "onlineNow",   online
        ));
    }
}