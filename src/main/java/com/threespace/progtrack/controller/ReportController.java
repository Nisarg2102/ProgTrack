package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.DailyTask;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.service.NotificationService;
import com.threespace.progtrack.service.TaskService;
import com.threespace.progtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
public class ReportController {

    @Autowired private TaskService taskService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    @GetMapping("/company")
    @PreAuthorize("hasAnyRole('CEO','DIRECTOR','ADMIN')")
    @Transactional(readOnly = true) // 🟢 Keeps DB session open
    public String companyReport(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String department,
            Model model) {

        User user = userService.getByEmail(principal.getUsername());
        LocalDate endDate   = (to   != null) ? to   : LocalDate.now();
        LocalDate startDate = (from != null) ? from : endDate.withDayOfMonth(1);

        List<DailyTask> tasks = (department != null && !department.isBlank())
                ? taskService.getDepartmentTasksByDateRange(department, startDate, endDate)
                : taskService.getAllTasksByDateRange(startDate, endDate);

        // 🟢 FIX FOR LAZY INITIALIZATION EXCEPTION
        if (tasks != null) {
            for (DailyTask t : tasks) {
                if (t != null && t.getUser() != null) {
                    t.getUser().getFullName();   // Forces Hibernate to fetch User
                    t.getUser().getDepartment(); // Forces Hibernate to fetch User
                }
            }
        }

        long totalTasks     = tasks.size();
        long completedTasks = tasks.stream().filter(t -> DailyTask.TaskStatus.COMPLETED.equals(t.getStatus())).count();
        long completionRate = totalTasks > 0 ? Math.round(completedTasks * 100.0 / totalTasks) : 0;
        double totalHours   = tasks.stream().mapToDouble(t -> t.getTimeSpent() != null ? t.getTimeSpent() : 0).sum();

        Map<String, Long> deptTaskCount = tasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getUser().getDepartment() != null ? t.getUser().getDepartment() : "Unknown",
                        Collectors.counting()));

        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (DailyTask.TaskStatus s : DailyTask.TaskStatus.values()) {
            statusBreakdown.put(s.name(), tasks.stream().filter(t -> s.equals(t.getStatus())).count());
        }

        model.addAttribute("user",            user);
        model.addAttribute("tasks",           tasks);
        model.addAttribute("from",            startDate);
        model.addAttribute("to",              endDate);
        model.addAttribute("selectedDept",    department);
        model.addAttribute("departments",     userService.getAllDepartments());
        model.addAttribute("totalTasks",      totalTasks);
        model.addAttribute("completedTasks",  completedTasks);
        model.addAttribute("completionRate",  completionRate);
        model.addAttribute("totalHours",      Math.round(totalHours * 100.0) / 100.0);
        model.addAttribute("deptTaskCount",   deptTaskCount);
        model.addAttribute("statusBreakdown", statusBreakdown);
        model.addAttribute("notifications",   notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount",notificationService.countUnseen(user));
        model.addAttribute("parentPage",        "Analytics");
        model.addAttribute("pageTitle",       "Company Report");
        return "reports/company-report";
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('LEAD', 'CEO','ADMIN')")
    @Transactional(readOnly = true) // 🟢 Keeps DB session open
    public String teamReport(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        User lead = userService.getByEmail(principal.getUsername());
        LocalDate endDate   = (to   != null) ? to   : LocalDate.now();
        LocalDate startDate = (from != null) ? from : endDate.minusDays(6);

        List<DailyTask> tasks = lead.getRole() == User.Role.LEAD
                ? taskService.getDepartmentTasksByDateRange(lead.getDepartment(), startDate, endDate)
                : taskService.getAllTasksByDateRange(startDate, endDate);

        // 🟢 FIX FOR LAZY INITIALIZATION EXCEPTION
        if (tasks != null) {
            for (DailyTask t : tasks) {
                if (t != null && t.getUser() != null) {
                    t.getUser().getFullName();
                    t.getUser().getDepartment();
                }
            }
        }

        long totalTasks      = tasks.size();
        long completedTasks  = tasks.stream().filter(t -> DailyTask.TaskStatus.COMPLETED.equals(t.getStatus())).count();
        long inProgressTasks = tasks.stream().filter(t -> DailyTask.TaskStatus.IN_PROGRESS.equals(t.getStatus())).count();
        long blockedTasks    = tasks.stream().filter(t -> DailyTask.TaskStatus.BLOCKED.equals(t.getStatus())).count();
        long completionRate  = totalTasks > 0 ? Math.round(completedTasks * 100.0 / totalTasks) : 0;
        double totalHours    = tasks.stream().mapToDouble(t -> t.getTimeSpent() != null ? t.getTimeSpent() : 0).sum();

        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (DailyTask.TaskStatus s : DailyTask.TaskStatus.values()) {
            statusBreakdown.put(s.name(), tasks.stream().filter(t -> s.equals(t.getStatus())).count());
        }

        Map<String, Long> memberTaskCount = tasks.stream()
                .filter(t -> t.getUser() != null && t.getUser().getFullName() != null)
                .collect(Collectors.groupingBy(t -> t.getUser().getFullName(), Collectors.counting()));

        model.addAttribute("user",             lead);
        model.addAttribute("tasks",            tasks);
        model.addAttribute("from",             startDate);
        model.addAttribute("to",               endDate);
        model.addAttribute("totalTasks",       totalTasks);
        model.addAttribute("completedTasks",   completedTasks);
        model.addAttribute("inProgressTasks",  inProgressTasks);
        model.addAttribute("blockedTasks",     blockedTasks);
        model.addAttribute("completionRate",   completionRate);
        model.addAttribute("totalHours",       Math.round(totalHours * 100.0) / 100.0);
        model.addAttribute("statusBreakdown",  statusBreakdown);
        model.addAttribute("memberTaskCount",  memberTaskCount);
        model.addAttribute("departments",      userService.getAllDepartments());
        model.addAttribute("notifications",    notificationService.getTop10(lead));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(lead));
        model.addAttribute("parentPage",        "Team");
        model.addAttribute("pageTitle",        "Team Analytics");
        return "reports/team-report";
    }

    @GetMapping("/productivity")
    @PreAuthorize("hasAnyRole('CEO','DIRECTOR','ADMIN')")
    public String productivityReport(
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        User user = userService.getByEmail(principal.getUsername());
        Map<String, Object> stats = taskService.getExecutiveDashboardStats();

        model.addAttribute("user",             user);
        model.addAttribute("stats",            stats);
        model.addAttribute("today",            LocalDate.now());
        model.addAttribute("departments",      userService.getAllDepartments());
        model.addAttribute("notifications",    notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(user));
        model.addAttribute("pageTitle",        "Productivity Analytics");
        return "reports/productivity";
    }
}