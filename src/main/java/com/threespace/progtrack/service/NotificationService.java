package com.threespace.progtrack.service;

import com.threespace.progtrack.entity.Notification;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public Notification createNotification(User user, String message, Notification.NotificationType type) {
        Notification n = new Notification(user, message, type);
        return notificationRepository.save(n);
    }

    public Notification createNotification(User user, String message, Notification.NotificationType type, String actionUrl) {
        Notification n = new Notification(user, message, type, actionUrl);
        return notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnseenNotifications(User user) {
        return notificationRepository.findByUserAndIsSeenFalseOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long countUnseen(User user) {
        return notificationRepository.countByUserAndIsSeenFalse(user);
    }

    @Transactional(readOnly = true)
    public List<Notification> getTop10(User user) {
        return notificationRepository.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    public void markAllSeen(User user) {
        notificationRepository.markAllSeenForUser(user);
    }

    public void markSeen(Long notificationId) {
        notificationRepository.markSeenById(notificationId);
    }
    
 // Add this method inside your NotificationService class

    @Transactional(readOnly = true)
    public Notification getById(Long id) {
        return notificationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
    }
    
 // Add this inside NotificationService
    @Transactional(readOnly = true)
    public boolean hasReminderBeenSentToday(User user) {
        LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        return notificationRepository.existsByUserAndTypeAndCreatedAtAfter(
                user, 
                Notification.NotificationType.REPORT_DEADLINE_REMINDER, 
                startOfDay
        );
    }
}
