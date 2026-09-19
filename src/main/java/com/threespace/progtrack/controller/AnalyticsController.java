package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.DailyTask;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.entity.WorkLog;
import com.threespace.progtrack.repository.WorkLogRepository;
import com.threespace.progtrack.service.NotificationService;
import com.threespace.progtrack.service.TaskService;
import com.threespace.progtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional; // <-- ADDED THIS IMPORT
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/analytics")
@PreAuthorize("hasAnyRole('CEO','DIRECTOR','ADMIN')")
public class AnalyticsController {

    @Autowired private TaskService taskService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;
    @Autowired private WorkLogRepository workLogRepository;

    // ─── Attendance / Work-Hours page ─────────────────────────────────────────
    @GetMapping("/attendance")
    @Transactional(readOnly = true) // <-- FIX: KEEPS DB SESSION OPEN FOR LAZY LOADING
    public String attendancePage(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String department,
            Model model) {

        User user = userService.getByEmail(principal.getUsername());

        LocalDate endDate   = (to   != null) ? to   : LocalDate.now();
        LocalDate startDate = (from != null) ? from : endDate.minusDays(29);

        List<User> members = (department != null && !department.isBlank())
                ? userService.getUsersByDepartment(department)
                : userService.getAllActiveUsers();

        // Build per-member attendance summary
        List<Map<String, Object>> attendanceSummary = new ArrayList<>();
        for (User member : members) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("user",         member);
            double hours = taskService.calculateOnlineHours(member, startDate, endDate);
            long days    = startDate.until(endDate).getDays() + 1;
            row.put("totalHours",   Math.round(hours * 100.0) / 100.0);
            row.put("avgHoursDay",  days > 0 ? Math.round((hours / days) * 100.0) / 100.0 : 0.0);
            row.put("taskCount",    taskService.getTasksByUserAndDateRange(member, startDate, endDate).size());
            attendanceSummary.add(row);
        }

        // Sort by totalHours desc
        attendanceSummary.sort((a, b) ->
                Double.compare((Double) b.get("totalHours"), (Double) a.get("totalHours")));

        // Chart data: member name → total hours
        Map<String, Double> hoursChartData = new LinkedHashMap<>();
        attendanceSummary.forEach(row -> {
            User u = (User) row.get("user");
            hoursChartData.put(u.getFullName(), (Double) row.get("totalHours"));
        });

        // Dept total hours
        Map<String, Double> deptHours = new LinkedHashMap<>();
        for (String dept : userService.getAllDepartments()) {
            double h = userService.getUsersByDepartment(dept).stream()
                    .mapToDouble(m -> taskService.calculateOnlineHours(m, startDate, endDate))
                    .sum();
            deptHours.put(dept, Math.round(h * 100.0) / 100.0);
        }

        double totalHoursAll = attendanceSummary.stream()
                .mapToDouble(r -> (Double) r.get("totalHours")).sum();

        long totalTasksAll = attendanceSummary.stream()
                .mapToLong(r -> (int) r.get("taskCount")).sum();

        double avgHoursPerEmployee = attendanceSummary.isEmpty() ? 0.0
                : Math.round((totalHoursAll / attendanceSummary.size()) * 100.0) / 100.0;

        model.addAttribute("user",                 user);
        model.addAttribute("attendanceSummary",    attendanceSummary);
        model.addAttribute("hoursChartData",       hoursChartData);
        model.addAttribute("deptHours",            deptHours);
        model.addAttribute("totalHoursAll",        Math.round(totalHoursAll * 100.0) / 100.0);
        model.addAttribute("totalTasksAll",        totalTasksAll);
        model.addAttribute("avgHoursPerEmployee",  avgHoursPerEmployee);
        model.addAttribute("from",                 startDate);
        model.addAttribute("to",                   endDate);
        model.addAttribute("selectedDept",         department);
        model.addAttribute("departments",          userService.getAllDepartments());
        model.addAttribute("notifications",        notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount",     notificationService.countUnseen(user));
        model.addAttribute("parentPage",        "Analytics");
        model.addAttribute("pageTitle",            "Attendance Analytics");

        return "analytics/attendance";
    }

    // ─── Productivity Trends page ─────────────────────────────────────────────
    @GetMapping("/productivity")
    @Transactional(readOnly = true) // <-- FIX: KEEPS DB SESSION OPEN FOR LAZY LOADING
    public String productivityPage(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String department,
            Model model) {

        User user = userService.getByEmail(principal.getUsername());

        LocalDate endDate   = (to   != null) ? to   : LocalDate.now();
        LocalDate startDate = (from != null) ? from : endDate.minusDays(29);

        // All tasks in range (optionally dept-filtered)
        List<DailyTask> tasks = (department != null && !department.isBlank())
                ? taskService.getDepartmentTasksByDateRange(department, startDate, endDate)
                : taskService.getAllTasksByDateRange(startDate, endDate);

        long totalTasks     = tasks.size();
        long completedTasks = tasks.stream()
                .filter(t -> DailyTask.TaskStatus.COMPLETED.equals(t.getStatus())).count();
        long completionRate = totalTasks > 0
                ? Math.round(completedTasks * 100.0 / totalTasks) : 0;
        double totalHoursLogged = tasks.stream()
                .mapToDouble(t -> t.getTimeSpent() != null ? t.getTimeSpent() : 0).sum();

        // ── SAFEGUARD: Prevent NullPointerException if taskDate is null ──
        Map<String, Long> dailyTaskCount = new TreeMap<>();
        tasks.forEach(t -> {
            if(t.getTaskDate() != null) {
                String d = t.getTaskDate().toString();
                dailyTaskCount.merge(d, 1L, Long::sum);
            }
        });

        // Status breakdown
        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (DailyTask.TaskStatus s : DailyTask.TaskStatus.values()) {
            statusBreakdown.put(s.name().replace("_", " "),
                    tasks.stream().filter(t -> s.equals(t.getStatus())).count());
        }

        // ── SAFEGUARD: Prevent NullPointerException if priority is missing ──
        Map<String, Long> priorityBreakdown = tasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPriority() != null ? t.getPriority().toString() : "MEDIUM",
                        Collectors.counting()));

        // ── SAFEGUARD: Prevent NullPointerException if user was deleted but task remains ──
        Map<String, Long> topContributors = tasks.stream()
                .filter(t -> t.getUser() != null && t.getUser().getFullName() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getUser().getFullName(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        // Category breakdown
        Map<String, Long> categoryBreakdown = tasks.stream()
                .filter(t -> t.getTaskCategory() != null && !t.getTaskCategory().isBlank())
                .collect(Collectors.groupingBy(DailyTask::getTaskCategory, Collectors.counting()));

        model.addAttribute("user",              user);
        model.addAttribute("totalTasks",        totalTasks);
        model.addAttribute("completedTasks",    completedTasks);
        model.addAttribute("completionRate",    completionRate);
        model.addAttribute("totalHoursLogged",  Math.round(totalHoursLogged * 100.0) / 100.0);
        model.addAttribute("dailyTaskCount",    dailyTaskCount);
        model.addAttribute("statusBreakdown",   statusBreakdown);
        model.addAttribute("priorityBreakdown", priorityBreakdown);
        model.addAttribute("topContributors",   topContributors);
        model.addAttribute("categoryBreakdown", categoryBreakdown);
        model.addAttribute("from",              startDate);
        model.addAttribute("to",                endDate);
        model.addAttribute("selectedDept",      department);
        model.addAttribute("departments",       userService.getAllDepartments());
        model.addAttribute("notifications",     notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount",  notificationService.countUnseen(user));
        model.addAttribute("parentPage",        "Analytics");
        model.addAttribute("pageTitle",         "Productivity Trends");

        return "analytics/productivity";
    }
}