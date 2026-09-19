package com.threespace.progtrack.repository;

import com.threespace.progtrack.entity.ChatMessage;
import com.threespace.progtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
        SELECT cm FROM ChatMessage cm
        WHERE (cm.sender = :user1 AND cm.recipient = :user2)
           OR (cm.sender = :user2 AND cm.recipient = :user1)
        ORDER BY cm.timestamp ASC
        """)
    List<ChatMessage> findConversation(@Param("user1") User user1, @Param("user2") User user2);

    @Query("""
        SELECT cm FROM ChatMessage cm
        WHERE (cm.sender = :user1 AND cm.recipient = :user2)
           OR (cm.sender = :user2 AND cm.recipient = :user1)
        ORDER BY cm.timestamp DESC
        LIMIT 50
        """)
    List<ChatMessage> findRecentConversation(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.recipient = :user AND cm.isRead = false")
    long countUnreadMessages(@Param("user") User user);

    @Query("""
            SELECT DISTINCT u FROM User u 
            WHERE u.id IN (
                SELECT cm.recipient.id FROM ChatMessage cm WHERE cm.sender.id = :userId
            ) OR u.id IN (
                SELECT cm.sender.id FROM ChatMessage cm WHERE cm.recipient.id = :userId
            )
        """)
        List<User> findConversationPartners(@Param("userId") Long userId);

    List<ChatMessage> findByRecipientAndIsReadFalse(User recipient);

    List<ChatMessage> findByRoomIdOrderByTimestampAsc(String roomId);
}
