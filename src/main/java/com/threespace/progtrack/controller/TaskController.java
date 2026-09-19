package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.DailyTask;
import com.threespace.progtrack.entity.LeaveRequest;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.UserRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired private TaskService taskService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository; // Added to fix the 500 Error
    @Autowired private NotificationService notificationService;

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('INTERN','LEAD', 'CEO','ADMIN')")
    @Transactional(readOnly = true)
    public String todayGrid(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.getByEmail(principal.getUsername());
        LocalDate today = LocalDate.now();

        List<DailyTask> existingTasks = taskService.getTasksByUserAndDate(user, today);
        
        if (existingTasks != null) {
            for (DailyTask t : existingTasks) {
                if (t != null && t.getUser() != null) {
                    t.getUser().getFullName();
                    t.getUser().getDepartment();
                }
            }
        }

        boolean alreadySubmitted      = taskService.isTodaySubmitted(user);
        long elapsedSeconds           = taskService.getCurrentSessionElapsedSeconds(user);

        model.addAttribute("user",              user);
        model.addAttribute("today",             today);
        model.addAttribute("existingTasks",     existingTasks);
        model.addAttribute("alreadySubmitted",  alreadySubmitted);
        model.addAttribute("elapsedSeconds",    elapsedSeconds);
        model.addAttribute("taskStatuses",      DailyTask.TaskStatus.values());
        model.addAttribute("leaveTypes",        LeaveRequest.LeaveType.values());
        model.addAttribute("notifications",     notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount",  notificationService.countUnseen(user));
        model.addAttribute("parentPage",        "My Workspace");
        model.addAttribute("pageTitle",         "Daily Task Report");
        return "task/daily-grid";
    }

    @PostMapping("/submit-daily")
    @PreAuthorize("hasAnyRole('INTERN','LEAD','CEO','ADMIN')")
    public String submitDailyGrid(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(value = "taskDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taskDate,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        User user      = userService.getByEmail(principal.getUsername());
        LocalDate date = (taskDate != null) ? taskDate : LocalDate.now();

        String[] descriptions = request.getParameterValues("taskDescription[]");
        String[] times        = request.getParameterValues("timeSpent[]");
        String[] statuses     = request.getParameterValues("status[]");
        String[] links        = request.getParameterValues("googleDocLink[]");
        String[] categories   = request.getParameterValues("taskCategory[]");
        String[] priorities   = request.getParameterValues("priority[]");

        List<TaskService.DailyTaskDTO> dtos = new ArrayList<>();
        
        if (descriptions != null) {
            for (int i = 0; i < descriptions.length; i++) {
                if (descriptions[i] == null || descriptions[i].trim().isEmpty()) {
                    continue;
                }
                
                TaskService.DailyTaskDTO dto = new TaskService.DailyTaskDTO();
                dto.setTaskDescription(descriptions[i].trim());
                
                double timeSpent = 0.0;
                if (times != null && i < times.length && times[i] != null && !times[i].isBlank()) {
                    try { timeSpent = Double.parseDouble(times[i]); } catch (NumberFormatException ignored) {}
                }
                dto.setTimeSpent(timeSpent);
                
                dto.setStatus(       statuses   != null && i < statuses.length   ? statuses[i]   : "IN_PROGRESS");
                dto.setGoogleDocLink(links      != null && i < links.length      ? links[i]      : null);
                dto.setTaskCategory( categories != null && i < categories.length ? categories[i] : null);
                dto.setPriority(     priorities != null && i < priorities.length ? priorities[i] : "MEDIUM");
                
                dtos.add(dto);
            }
        }

        try {
            List<DailyTask> saved = taskService.submitDailyTaskGrid(user, date, dtos);
            redirectAttributes.addFlashAttribute("successMsg",
                    "✅ Daily report submitted! " + saved.size() + " task(s) recorded.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "❌ Submission failed: " + e.getMessage());
        }
        return "redirect:/tasks/today";
    }

    @GetMapping("/history")
    @Transactional(readOnly = true)
    public String taskHistory(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        User user           = userService.getByEmail(principal.getUsername());
        LocalDate endDate   = (to   != null) ? to   : LocalDate.now();
        LocalDate startDate = (from != null) ? from : endDate.minusDays(29);
        List<DailyTask> tasks = taskService.getTasksByUserAndDateRange(user, startDate, endDate);

        if (tasks != null) {
            for (DailyTask t : tasks) {
                if (t != null && t.getUser() != null) {
                    t.getUser().getFullName();
                    t.getUser().getDepartment();
                }
            }
        }

        long completedCount = tasks.stream()
                .filter(t -> DailyTask.TaskStatus.COMPLETED.equals(t.getStatus())).count();
        double totalHours = tasks.stream()
                .mapToDouble(t -> t.getTimeSpent() != null ? t.getTimeSpent() : 0).sum();

        model.addAttribute("user",              user);
        model.addAttribute("tasks",             tasks);
        model.addAttribute("from",              startDate);
        model.addAttribute("to",                endDate);
        model.addAttribute("completedCount",    completedCount);
        model.addAttribute("totalHours",        Math.round(totalHours * 100.0) / 100.0);
        model.addAttribute("notifications",     notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount",  notificationService.countUnseen(user));
        model.addAttribute("parentPage",        "My Workspace");
        model.addAttribute("pageTitle",         "Task History");
        return "task/history";
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('LEAD', 'CEO','ADMIN')")
    @Transactional(readOnly = true)
    public String teamTasks(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        User lead         = userService.getByEmail(principal.getUsername());
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        String dept = lead.getDepartment();

        List<DailyTask> tasks;
        if (lead.getRole() == User.Role.CEO
                || lead.getRole() == User.Role.ADMIN) {
            tasks = taskService.getAllTasksByDateRange(targetDate, targetDate);
        } else {
            tasks = taskService.getDepartmentTasksByDate(dept, targetDate);
        }

        if (tasks != null) {
            for (DailyTask t : tasks) {
                if (t != null && t.getUser() != null) {
                    t.getUser().getFullName();
                    t.getUser().getDepartment();
                    t.getUser().getDesignation();
                }
            }
        }

        long totalCount      = tasks.size();
        long completedCount  = tasks.stream().filter(t -> DailyTask.TaskStatus.COMPLETED.equals(t.getStatus())).count();
        long inProgressCount = tasks.stream().filter(t -> DailyTask.TaskStatus.IN_PROGRESS.equals(t.getStatus())).count();
        long blockedCount    = tasks.stream().filter(t -> DailyTask.TaskStatus.BLOCKED.equals(t.getStatus())).count();
        long criticalCount   = tasks.stream().filter(t -> "CRITICAL".equals(t.getPriority())).count();
        double totalHours    = tasks.stream().mapToDouble(t -> t.getTimeSpent() != null ? t.getTimeSpent() : 0).sum();

        model.addAttribute("user",              lead);
        model.addAttribute("tasks",             tasks);
        model.addAttribute("targetDate",        targetDate);
        model.addAttribute("totalCount",        totalCount);
        model.addAttribute("completedCount",    completedCount);
        model.addAttribute("inProgressCount",   inProgressCount);
        model.addAttribute("blockedCount",      blockedCount);
        model.addAttribute("criticalCount",     criticalCount);
        model.addAttribute("totalHours",        Math.round(totalHours * 100.0) / 100.0);
        model.addAttribute("notifications",     notificationService.getTop10(lead));
        model.addAttribute("unreadNotifCount",  notificationService.countUnseen(lead));
        model.addAttribute("parentPage",        "Team");
        model.addAttribute("pageTitle",         "Team Report — " + targetDate);
        
        List<User> teamMembers = userRepository.findActiveUsersByDepartment(dept);
        model.addAttribute("teamMembers", teamMembers);
        
        return "task/team-report";
    }

    @GetMapping("/api/elapsed-seconds")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getElapsedSeconds(
            @AuthenticationPrincipal UserDetails principal) {
        User user       = userService.getByEmail(principal.getUsername());
        
        // CHANGED: Synchronize the DB with active time whenever the frontend polls
        taskService.syncActiveSession(user); 
        
        long seconds    = taskService.getCurrentSessionElapsedSeconds(user);
        long todayTotal = taskService.calculateTodayOnlineSeconds(user);
        
        return ResponseEntity.ok(Map.of(
                "elapsedSeconds",    seconds,
                "todayTotalSeconds", todayTotal,
                "formatted",         taskService.formatSeconds(seconds)));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getByEmail(principal.getUsername());
        try {
            taskService.deleteTask(id, user);
            return ResponseEntity.ok(Map.of("status", "deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}