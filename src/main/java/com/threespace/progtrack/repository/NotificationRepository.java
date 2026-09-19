package com.threespace.progtrack.repository;

import com.threespace.progtrack.entity.Notification;
import com.threespace.progtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndIsSeenFalseOrderByCreatedAtDesc(User user);

    long countByUserAndIsSeenFalse(User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isSeen = true, n.seenAt = CURRENT_TIMESTAMP WHERE n.user = :user AND n.isSeen = false")
    void markAllSeenForUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isSeen = true, n.seenAt = CURRENT_TIMESTAMP WHERE n.id = :id")
    void markSeenById(@Param("id") Long id);

    @Query("SELECT n FROM Notification n WHERE n.user = :user ORDER BY n.createdAt DESC LIMIT 10")
    List<Notification> findTop10ByUserOrderByCreatedAtDesc(@Param("user") User user);
    
 // Add this inside NotificationRepository
    boolean existsByUserAndTypeAndCreatedAtAfter(User user, Notification.NotificationType type, java.time.LocalDateTime time);
}
