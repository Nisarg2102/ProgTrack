package com.threespace.progtrack.websocket;

import com.threespace.progtrack.entity.ChatMessage;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.service.ChatService;
import com.threespace.progtrack.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.threespace.progtrack.service.NotificationService;
import com.threespace.progtrack.entity.Notification;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket STOMP controller for the internal chat system.
 *
 * Message flows:
 *  Client → /app/chat.sendMessage   → persisted → delivered to /user/{recipient}/queue/messages
 *  Client → /app/chat.addUser       → broadcast join event to /topic/public
 *  Client → /app/chat.typing        → broadcast typing indicator to recipient
 */
@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private NotificationService notificationService;

    // =====================
    // SEND PRIVATE MESSAGE
    // =====================

    /**
     * Client sends: { "recipientEmail": "...", "content": "..." }
     * Message is persisted and delivered to recipient's personal queue.
     */
    @MessageMapping("/chat.sendMessage")
    public void sendPrivateMessage(@Payload ChatMessagePayload payload,
                                   Principal principal,
                                   SimpMessageHeaderAccessor headerAccessor) {
        if (principal == null) {
            log.warn("⚠️ Unauthenticated WebSocket message attempt blocked.");
            return;
        }

        String senderEmail = principal.getName();

        try {
            User sender = userService.getByEmail(senderEmail);
            User recipient = userService.getByEmail(payload.getRecipientEmail());

            // Sanitize content — text and links only
            String content = sanitize(payload.getContent());
            if (content.isBlank()) return;

            // Persist to database
            ChatMessage saved = chatService.sendMessage(sender, recipient, content);
            
         // 🟢 Create Notification for Private Chat
            notificationService.createNotification(
                recipient,
                "New message from " + sender.getFullName(),
                Notification.NotificationType.CHAT_MESSAGE,
                "/chat/conversation/" + sender.getId()
            );
            
         

            // Build response payload
            ChatMessageResponse response = buildResponse(saved, sender);

            // Deliver to recipient's personal queue
            messagingTemplate.convertAndSendToUser(
                recipient.getEmail(),
                "/queue/messages",
                response
            );

            // Echo back to sender so they see their own message in real time
            messagingTemplate.convertAndSendToUser(
                sender.getEmail(),
                "/queue/messages",
                response
            );

            log.debug("💬 Message sent: {} → {} ({} chars)",
                senderEmail, payload.getRecipientEmail(), content.length());

        } catch (Exception e) {
            log.error("❌ Error processing chat message from {}: {}", senderEmail, e.getMessage(), e);
            sendErrorToUser(senderEmail, "Failed to send message: " + e.getMessage());
        }
    }

    // =====================
    // TYPING INDICATOR
    // =====================

    /**
     * Client sends: { "recipientEmail": "...", "typing": true/false }
     */
    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload TypingPayload payload, Principal principal) {
        if (principal == null) return;

        String senderEmail = principal.getName();
        try {
            User sender = userService.getByEmail(senderEmail);

            Map<String, Object> typingEvent = new HashMap<>();
            typingEvent.put("type", "TYPING");
            typingEvent.put("senderEmail", senderEmail);
            typingEvent.put("senderName", sender.getFullName());
            typingEvent.put("isTyping", payload.isTyping());

            messagingTemplate.convertAndSendToUser(
                payload.getRecipientEmail(),
                "/queue/typing",
                typingEvent
            );
        } catch (Exception e) {
            log.debug("Typing indicator error (non-critical): {}", e.getMessage());
        }
    }

    // =====================
    // USER JOIN (public broadcast)
    // =====================

    /**
     * Broadcast when a user opens the chat page.
     * Delivers to /topic/public
     */
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload UserJoinPayload payload, Principal principal) {
        if (principal == null) return;

        try {
            User user = userService.getByEmail(principal.getName());

            Map<String, Object> joinEvent = new HashMap<>();
            joinEvent.put("type", "USER_JOINED");
            joinEvent.put("userEmail", user.getEmail());
            joinEvent.put("userName", user.getFullName());
            joinEvent.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));

            messagingTemplate.convertAndSend("/topic/public", joinEvent);
            log.debug("👤 User joined chat: {}", user.getFullName());
        } catch (Exception e) {
            log.debug("User join event error (non-critical): {}", e.getMessage());
        }
    }

    // =====================
    // MARK READ
    // =====================

    /**
     * Client sends: { "senderEmail": "..." } to acknowledge reading messages.
     */
    @MessageMapping("/chat.markRead")
    public void markRead(@Payload MarkReadPayload payload, Principal principal) {
        if (principal == null) return;
        try {
            // 1. Identify who is reading (recipient) and whose messages were read (sender)
            User recipient = userService.getByEmail(principal.getName());
            User sender = userService.getByEmail(payload.getSenderEmail());
            
            // 2. Update the database so they stay read on refresh
            chatService.markMessagesRead(sender, recipient);

            // 3. THE FIX: Send a real-time ping back to the sender to turn their ticks blue!
            messagingTemplate.convertAndSendToUser(
                sender.getEmail(), 
                "/queue/readReceipts", 
                Map.of("read", true, "readerEmail", recipient.getEmail())
            );
            
        } catch (Exception e) {
            log.debug("MarkRead error (non-critical): {}", e.getMessage());
        }
    }

    // =====================
    // HELPERS
    // =====================

    private ChatMessageResponse buildResponse(ChatMessage saved, User sender) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(saved.getId());
        response.setSenderEmail(sender.getEmail());
        response.setSenderName(sender.getFullName());
        response.setRecipientEmail(saved.getRecipient() != null ? saved.getRecipient().getEmail() : null);
        response.setContent(saved.getContent());
        response.setTimestamp(saved.getTimestamp().format(TIMESTAMP_FORMAT));
        response.setFullTimestamp(saved.getTimestamp().toString());
        response.setMessageType(saved.getMessageType() != null ? saved.getMessageType().name() : "TEXT");
        response.setIsRead(saved.getIsRead());
        return response;
    }

    private void sendErrorToUser(String email, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", "ERROR");
        error.put("message", errorMessage);
        messagingTemplate.convertAndSendToUser(email, "/queue/errors", error);
    }

    private String sanitize(String content) {
        if (content == null) return "";
        // Strip HTML — allow plain text and URLs only
        return content.replaceAll("<[^>]*>", "").trim();
    }

    // =====================
    // INNER PAYLOAD CLASSES
    // =====================

    public static class ChatMessagePayload {
        private String recipientEmail;
        private String content;

        public ChatMessagePayload() {}

        public String getRecipientEmail() { return recipientEmail; }
        public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class TypingPayload {
        private String recipientEmail;
        private boolean typing;

        public TypingPayload() {}

        public String getRecipientEmail() { return recipientEmail; }
        public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

        public boolean isTyping() { return typing; }
        public void setTyping(boolean typing) { this.typing = typing; }
    }

    public static class UserJoinPayload {
        private String userEmail;

        public UserJoinPayload() {}

        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    }

    public static class MarkReadPayload {
        private String senderEmail;

        public MarkReadPayload() {}

        public String getSenderEmail() { return senderEmail; }
        public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    }

    public static class ChatMessageResponse {
        private Long id;
        private String senderEmail;
        private String senderName;
        private String recipientEmail;
        private String content;
        private String timestamp;
        private String fullTimestamp;
        private String messageType;
        private Boolean isRead;

        public ChatMessageResponse() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getSenderEmail() { return senderEmail; }
        public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }

        public String getRecipientEmail() { return recipientEmail; }
        public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getFullTimestamp() { return fullTimestamp; }
        public void setFullTimestamp(String fullTimestamp) { this.fullTimestamp = fullTimestamp; }

        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }

        public Boolean getIsRead() { return isRead; }
        public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    }
    
 // =====================
    // SEND GROUP MESSAGE
    // =====================
    @MessageMapping("/chat.sendGroupMessage")
    public void sendGroupMessage(@Payload GroupMessagePayload payload, Principal principal) {
        if (principal == null) return;
        String senderEmail = principal.getName();
        try {
            User sender = userService.getByEmail(senderEmail);
            String content = sanitize(payload.getContent());
            if (content.isBlank()) return;

            // Persist to database
            ChatMessage saved = chatService.sendGroupMessage(sender, payload.getRoomId(), content);

            ChatMessageResponse response = buildResponse(saved, sender);
            response.setRecipientEmail(payload.getRoomId()); // repurpose field for frontend sorting

            // Broadcast to all users subscribed to this topic
            messagingTemplate.convertAndSend("/topic/" + payload.getRoomId(), response);
            
         // 🟢 Create Notification for Group Broadcasts
            List<User> allUsers = userService.getAllActiveUsers();
            allUsers.stream()
                .filter(u -> !u.getId().equals(sender.getId())) // Don't notify the sender
                .filter(u -> {
                    if (payload.getRoomId().equals("ANNOUNCEMENTS")) return true;
                    if (payload.getRoomId().equals("MANAGEMENT")) {
                        return u.getRole().name().equals("CEO") || 
                               u.getRole().name().equals("LEAD");
                    }
                    if (payload.getRoomId().equals("TEAM_SYNC")) {
                        return sender.getDepartment() != null && sender.getDepartment().equals(u.getDepartment());
                    }
                    return false;
                })
                .forEach(u -> notificationService.createNotification(
                    u,
                    "New broadcast in " + payload.getRoomId(),
                    Notification.NotificationType.CHAT_MESSAGE,
                    "/chat/group/" + payload.getRoomId()
                ));
            
        

            log.debug("📢 Group Message sent to {}: {}", payload.getRoomId(), content);
        } catch (Exception e) {
            log.error("❌ Error sending group message: {}", e.getMessage());
        }
    }

    // 👉 ADD THIS INNER CLASS AT THE BOTTOM OF THE FILE WITH THE OTHERS:
    public static class GroupMessagePayload {
        private String roomId;
        private String content;
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
