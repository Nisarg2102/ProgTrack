package com.threespace.progtrack.service;

import com.threespace.progtrack.entity.DailyTask;
import com.threespace.progtrack.entity.Notification;
import com.threespace.progtrack.entity.ReportSubmission;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.entity.WorkLog;
import com.threespace.progtrack.repository.DailyTaskRepository;
import com.threespace.progtrack.repository.NotificationRepository;
import com.threespace.progtrack.repository.ReportSubmissionRepository;
import com.threespace.progtrack.repository.UserRepository;
import com.threespace.progtrack.repository.WorkLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private DailyTaskRepository dailyTaskRepository;

    @Autowired
    private WorkLogRepository workLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReportSubmissionRepository reportSubmissionRepository;

    // =========================================================
    // SECTION 1 — Daily Task Grid Operations
    // =========================================================

    public List<DailyTask> submitDailyTaskGrid(User user, LocalDate taskDate, List<DailyTaskDTO> taskDTOs) {
        log.info("📋 Submitting daily task grid for user: {} date: {}", user.getEmail(), taskDate);

        List<DailyTask> existingUnsubmitted = dailyTaskRepository.findByUserAndSubmittedFalse(user);
        existingUnsubmitted.stream()
            .filter(t -> t.getTaskDate().equals(taskDate))
            .forEach(dailyTaskRepository::delete);

        dailyTaskRepository.flush();

        List<DailyTask> savedTasks = new ArrayList<>();
        for (DailyTaskDTO dto : taskDTOs) {
            if (dto.getTaskDescription() == null || dto.getTaskDescription().trim().isEmpty()) {
                continue; 
            }

            DailyTask task = new DailyTask();
            task.setUser(user);
            task.setTaskDate(taskDate);
            task.setTaskDescription(dto.getTaskDescription().trim());
            task.setTimeSpent(dto.getTimeSpent() != null ? dto.getTimeSpent() : 0.0);
            task.setStatus(parseStatus(dto.getStatus()));
            task.setGoogleDocLink(dto.getGoogleDocLink());
            task.setTaskCategory(dto.getTaskCategory());
            task.setPriority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM");
            task.setSubmitted(true);

            savedTasks.add(dailyTaskRepository.save(task));
        }

        notifyLeadOnTaskSubmission(user, taskDate, savedTasks.size());

        log.info("✅ Saved {} task rows for {}", savedTasks.size(), user.getEmail());
        return savedTasks;
    }

    public DailyTask saveTask(DailyTask task) {
        return dailyTaskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<DailyTask> getTasksByUserAndDate(User user, LocalDate date) {
        return dailyTaskRepository.findByUserAndTaskDate(user, date);
    }

    @Transactional(readOnly = true)
    public List<DailyTask> getTasksByUserAndDateRange(User user, LocalDate from, LocalDate to) {
        return dailyTaskRepository.findByUserAndTaskDateBetweenOrderByTaskDateDesc(user, from, to);
    }

    @Transactional(readOnly = true)
    public List<DailyTask> getDepartmentTasksByDate(String department, LocalDate date) {
        return dailyTaskRepository.findByDepartmentAndDate(department, date);
    }

    @Transactional(readOnly = true)
    public List<DailyTask> getDepartmentTasksByDateRange(String department, LocalDate from, LocalDate to) {
        return dailyTaskRepository.findByDepartmentAndDateRange(department, from, to);
    }

    @Transactional(readOnly = true)
    public List<DailyTask> getAllTasksByDateRange(LocalDate from, LocalDate to) {
        return dailyTaskRepository.findAllByDateRange(from, to);
    }

    @Transactional(readOnly = true)
    public boolean isTodaySubmitted(User user) {
        return dailyTaskRepository.existsByUserAndTaskDateAndSubmitted(user, LocalDate.now(), true);
    }

    public void deleteTask(Long taskId, User user) {
        DailyTask task = dailyTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You are not authorized to delete this task.");
        }
        if (Boolean.TRUE.equals(task.getSubmitted())) {
            throw new IllegalStateException("Cannot delete a submitted task.");
        }
        dailyTaskRepository.delete(task);
    }

    // =========================================================
    // SECTION 2 — Work Time Auto-Calculation & Synchronization
    // =========================================================

    /**
     * CHANGED: Force sync current elapsed time to the database for crash/restart recovery.
     */
    public void syncActiveSession(User user) {
        workLogRepository.findByUserOrderByLoginDateTimeDesc(user).stream()
            .filter(wl -> wl.getLogoutDateTime() == null)
            .findFirst()
            .ifPresent(wl -> {
                long elapsed = ChronoUnit.SECONDS.between(wl.getLoginDateTime(), LocalDateTime.now());
                wl.setTotalSeconds(Math.max(0, elapsed));
                workLogRepository.save(wl);
            });
    }

    /**
     * CHANGED: Unified, bulletproof logic that auto-heals lost sessions and properly splits base time from current time.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getOnlineTimeStats(User user, LocalDateTime from, LocalDateTime to) {
        List<WorkLog> logs = workLogRepository.findByUserAndLoginDateTimeBetween(user, from, to);
        long baseSeconds = 0;
        long sessionSeconds = 0;

        WorkLog activeLog = logs.stream()
            .filter(wl -> wl.getLogoutDateTime() == null)
            .max(Comparator.comparing(WorkLog::getLoginDateTime))
            .orElse(null);

        for (WorkLog wl : logs) {
            if (wl.equals(activeLog)) {
                // Current live session
                long rawElapsed = ChronoUnit.SECONDS.between(wl.getLoginDateTime(), LocalDateTime.now());
                sessionSeconds = Math.max(0, rawElapsed);
            } else {
                // Previously completed OR abandoned sessions that saved their totalSeconds via heartbeat
                baseSeconds += (wl.getTotalSeconds() != null ? wl.getTotalSeconds() : 0);
            }
        }

        Map<String, Long> stats = new HashMap<>();
        stats.put("baseSeconds", baseSeconds); // Total past time
        stats.put("sessionSeconds", sessionSeconds); // Total live time
        stats.put("totalSeconds", baseSeconds + sessionSeconds); // Total overall time
        return stats;
    }

    @Transactional(readOnly = true)
    public long calculateTodayOnlineSeconds(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return getOnlineTimeStats(user, startOfDay, LocalDateTime.now()).get("totalSeconds");
    }

    @Transactional(readOnly = true)
    public double calculateOnlineHours(User user, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        long totalSeconds = getOnlineTimeStats(user, start, end).get("totalSeconds");
        if (totalSeconds <= 0) return 0.0;
        return Math.round((totalSeconds / 3600.0) * 100.0) / 100.0;
    }

    public String formatSeconds(long totalSeconds) {
        if (totalSeconds <= 0) return "0h 0m 0s";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    @Transactional(readOnly = true)
    public long getCurrentSessionElapsedSeconds(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return getOnlineTimeStats(user, startOfDay, LocalDateTime.now()).get("sessionSeconds");
    }

    @Transactional(readOnly = true)
    public List<WorkLog> getWorkLogsByUserAndDateRange(User user, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return workLogRepository.findByUserAndLoginDateTimeBetween(user, start, end);
    }

    // =========================================================
    // SECTION 3 — Dashboard Summary Stats
    // =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getInternDashboardStats(User user) {
        Map<String, Object> stats = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);

        // Today's tasks
        List<DailyTask> todayTasks = dailyTaskRepository.findByUserAndTaskDate(user, today);
        stats.put("todayTaskCount", todayTasks.size());
        stats.put("todayTasks", todayTasks);
        stats.put("isTodaySubmitted", isTodaySubmitted(user));

        // Time online today
        Map<String, Long> timeStats = getOnlineTimeStats(user, today.atStartOfDay(), LocalDateTime.now());
        long onlineSeconds = timeStats.get("totalSeconds");
        stats.put("onlineSecondsToday", onlineSeconds);
        stats.put("onlineHoursToday", Math.round(onlineSeconds / 3600.0 * 100.0) / 100.0);
        stats.put("formattedOnlineTime", formatSeconds(onlineSeconds));
        // Provide strict baseline metric so JS doesn't double count
        stats.put("baseOnlineSeconds", timeStats.get("baseSeconds")); 

        // This week
        Map<String, Long> weekTimeStats = getOnlineTimeStats(user, weekStart.atStartOfDay(), today.plusDays(1).atStartOfDay());
        double weekHoursTotal = Math.round((weekTimeStats.get("totalSeconds") / 3600.0) * 100.0) / 100.0;
        double weekHoursBase = Math.round((weekTimeStats.get("baseSeconds") / 3600.0) * 100.0) / 100.0;
        
        stats.put("weekOnlineHours", weekHoursTotal);
        stats.put("baseWeekOnlineHours", weekHoursBase);

        // Task totals this week
        List<DailyTask> weekTasks = dailyTaskRepository
            .findByUserAndTaskDateBetweenOrderByTaskDateDesc(user, weekStart, today);
        stats.put("weekTaskCount", weekTasks.size());
        
     // >>> ADD THESE TWO NEW LINES HERE: <<<
        double weekTaskHours = weekTasks.stream().mapToDouble(t -> t.getTimeSpent() != null ? t.getTimeSpent() : 0.0).sum();
        stats.put("weekTaskHours", Math.round(weekTaskHours * 100.0) / 100.0);

        long completedThisWeek = weekTasks.stream()
            .filter(t -> DailyTask.TaskStatus.COMPLETED.equals(t.getStatus()))
            .count();
        stats.put("completedThisWeek", completedThisWeek);

     // ==========================================
        // NEW ADDITIONS: Report Counters
        // ==========================================
        LocalDate monthStart = today.withDayOfMonth(1);
        
        // Fetch tasks for the current month
        List<DailyTask> monthTasks = dailyTaskRepository
            .findByUserAndTaskDateBetweenOrderByTaskDateDesc(user, monthStart, today);

        // 1. Daily Report Submitted (1 if today is submitted, else 0)
        long dailyReports = isTodaySubmitted(user) ? 1 : 0;

        // 2. Weekly Reports Submitted (Count actual uploaded WEEKLY documents)
        long weeklyReports = reportSubmissionRepository
            .findByUserAndTypeOrderBySubmittedAtDesc(user, ReportSubmission.ReportType.WEEKLY)
            .size();

        // 3. Monthly Reports Submitted (Count actual uploaded MONTHLY documents)
        long monthlyReports = reportSubmissionRepository
            .findByUserAndTypeOrderBySubmittedAtDesc(user, ReportSubmission.ReportType.MONTHLY)
            .size();

        stats.put("dailyReports", dailyReports);
        stats.put("weeklyReports", weeklyReports);
        stats.put("monthlyReports", monthlyReports);
        
        
        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLeadDashboardStats(User lead) {
        Map<String, Object> stats = new LinkedHashMap<>();
        String department = lead.getDepartment();

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);

        List<User> teamMembers = userRepository.findActiveUsersByDepartment(department);
        stats.put("teamSize", teamMembers.size());
        stats.put("teamMembers", teamMembers);

List<DailyTask> todayDeptTasks = dailyTaskRepository.findByDepartmentAndDate(department, today);
        
        // FIX: Count the distinct number of interns who submitted today, not the total number of tasks
        long distinctSubmissionsToday = todayDeptTasks.stream()
            .map(DailyTask::getUser)
            .distinct()
            .count();
            
        stats.put("todaySubmissions", distinctSubmissionsToday);

        Map<String, Double> memberHours = new LinkedHashMap<>();
        for (User member : teamMembers) {
            double hours = calculateOnlineHours(member, weekStart, today);
            memberHours.put(member.getFullName(), hours);
        }
        stats.put("memberWeeklyHours", memberHours);

        List<DailyTask> weekTasks = dailyTaskRepository.findByDepartmentAndDateRange(department, weekStart, today);
        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (DailyTask.TaskStatus status : DailyTask.TaskStatus.values()) {
            long count = weekTasks.stream().filter(t -> status.equals(t.getStatus())).count();
            statusBreakdown.put(status.name(), count);
        }
        stats.put("statusBreakdown", statusBreakdown);
        stats.put("totalWeekTasks", (long) weekTasks.size());

        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getExecutiveDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        Map<String, Long> headcount = new LinkedHashMap<>();
        for (User.Role role : User.Role.values()) {
            headcount.put(role.name(), userRepository.countActiveUsersByRole(role));
        }
        stats.put("headcount", headcount);
        stats.put("totalActive", userRepository.findByIsActiveTrue().size());

        List<DailyTask> monthTasks = dailyTaskRepository.findAllByDateRange(monthStart, today);
        stats.put("monthTaskCount", monthTasks.size());

        long completedMonth = monthTasks.stream()
            .filter(t -> DailyTask.TaskStatus.COMPLETED.equals(t.getStatus()))
            .count();
        stats.put("completionRate",
            monthTasks.isEmpty() ? 0 : Math.round((completedMonth * 100.0) / monthTasks.size()));

        List<String> departments = userRepository.findAllDepartments();
        Map<String, Long> deptTaskCount = new LinkedHashMap<>();
        for (String dept : departments) {
            long count = dailyTaskRepository
                .findByDepartmentAndDateRange(dept, monthStart, today).size();
            deptTaskCount.put(dept, count);
        }
        stats.put("departmentTaskCount", deptTaskCount);
        stats.put("departments", departments);

        // 🟢 FIX: Fetch all active leads and add them to the stats map for the Directory!
        List<User> leads = userRepository.findByIsActiveTrue().stream()
                .filter(u -> User.Role.LEAD.equals(u.getRole()))
                .collect(Collectors.toList());
        stats.put("leads", leads);

        return stats;
    }

    // =========================================================
    // SECTION 4 — Private Helpers
    // =========================================================

    private DailyTask.TaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return DailyTask.TaskStatus.IN_PROGRESS;
        try {
            return DailyTask.TaskStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DailyTask.TaskStatus.IN_PROGRESS;
        }
    }

    private void notifyLeadOnTaskSubmission(User intern, LocalDate date, int taskCount) {
        try {
            userRepository.findActiveUsersByDepartment(intern.getDepartment())
                .stream()
                .filter(u -> User.Role.LEAD.equals(u.getRole()))
                .forEach(lead -> {
                    Notification notif = new Notification(
                        lead,
                        intern.getFullName() + " submitted " + taskCount + " task(s) for " + date,
                        Notification.NotificationType.TASK_SUBMITTED,
                        
                        // 🟢 FIX: Corrected URL to point to /tasks/team and pass the date and intern's name!
                        "/tasks/team?date=" + date + "&intern=" + intern.getFullName()
                    );
                    notificationRepository.save(notif);
                });
        } catch (Exception e) {
            log.warn("Could not send lead notification for task submission: {}", e.getMessage());
        }
    }

    // =========================================================
    // SECTION 5 — Inner DTO for grid submission
    // =========================================================

    public static class DailyTaskDTO {
        private String taskDescription;
        private Double timeSpent;
        private String status;
        private String googleDocLink;
        private String taskCategory;
        private String priority;

        public DailyTaskDTO() {}

        public DailyTaskDTO(String taskDescription, Double timeSpent, String status,
                            String googleDocLink, String taskCategory, String priority) {
            this.taskDescription = taskDescription;
            this.timeSpent = timeSpent;
            this.status = status;
            this.googleDocLink = googleDocLink;
            this.taskCategory = taskCategory;
            this.priority = priority;
        }

        public String getTaskDescription() { return taskDescription; }
        public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }

        public Double getTimeSpent() { return timeSpent; }
        public void setTimeSpent(Double timeSpent) { this.timeSpent = timeSpent; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getGoogleDocLink() { return googleDocLink; }
        public void setGoogleDocLink(String googleDocLink) { this.googleDocLink = googleDocLink; }

        public String getTaskCategory() { return taskCategory; }
        public void setTaskCategory(String taskCategory) { this.taskCategory = taskCategory; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        @Override
        public String toString() {
            return "DailyTaskDTO{taskDescription='" + taskDescription + "', timeSpent=" + timeSpent + ", status='" + status + "'}";
        }
    }
}