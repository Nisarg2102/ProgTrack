package com.threespace.progtrack.scheduler;

import com.threespace.progtrack.entity.Notification;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.UserRepository;
import com.threespace.progtrack.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReportReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportReminderScheduler.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * WEEKLY REMINDER
     * Runs every hour on the hour from 9:00 AM to 5:00 PM on Thursdays.
     * Cron expression: "0 0 9-17 * * THU"
     */
    @Scheduled(cron = "0 0 9-17 * * THU")
    public void sendWeeklyReportReminders() {
        List<User> activeInterns = getActiveInterns();
        if (activeInterns.isEmpty()) return;

        int sentCount = 0;
        for (User intern : activeInterns) {
            // FIX: Check if we already sent the reminder today!
            if (!notificationService.hasReminderBeenSentToday(intern)) {
                notificationService.createNotification(
                        intern,
                        "⏳ Reminder: Your Weekly Report is due tomorrow (Friday) by 5:30 PM.",
                        Notification.NotificationType.REPORT_DEADLINE_REMINDER,
                        "/workspace/reports/weekly"
                );
                sentCount++;
            }
        }
        
        if (sentCount > 0) {
            log.info("✅ Sent weekly reminders to {} interns.", sentCount);
        }
    }

    /**
     * MONTHLY REMINDER
     * Runs every hour on the hour from 9:00 AM to 5:00 PM every day.
     * Only executes logic if tomorrow is the last day of the month.
     */
    @Scheduled(cron = "0 0 9-17 * * *")
    public void sendMonthlyReportReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Check if tomorrow is the last day of the month
        if (tomorrow.getDayOfMonth() == tomorrow.lengthOfMonth()) {
            List<User> activeInterns = getActiveInterns();
            if (activeInterns.isEmpty()) return;

            int sentCount = 0;
            for (User intern : activeInterns) {
                // FIX: Check if we already sent the reminder today!
                if (!notificationService.hasReminderBeenSentToday(intern)) {
                    notificationService.createNotification(
                            intern,
                            "⏳ Reminder: Your Monthly Report is due tomorrow (Last day of the month) by 5:30 PM.",
                            Notification.NotificationType.REPORT_DEADLINE_REMINDER,
                            "/workspace/reports/monthly"
                    );
                    sentCount++;
                }
            }
            
            if (sentCount > 0) {
                log.info("✅ Sent monthly reminders to {} interns.", sentCount);
            }
        }
    }

    private List<User> getActiveInterns() {
        return userRepository.findAll().stream()
                .filter(u -> User.Role.INTERN.equals(u.getRole()) && Boolean.TRUE.equals(u.getIsActive()))
                .collect(Collectors.toList());
    }
}