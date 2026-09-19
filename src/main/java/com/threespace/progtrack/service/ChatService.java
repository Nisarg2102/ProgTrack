package com.threespace.progtrack.service;

import com.threespace.progtrack.entity.ChatMessage;
import com.threespace.progtrack.entity.Notification;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.ChatMessageRepository;
import com.threespace.progtrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    public ChatMessage sendMessage(User sender, User recipient, String content) {
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(sanitize(content));
        message.setTimestamp(LocalDateTime.now());
        message.setIsRead(false);

        // Detect if content is a URL
        if (isLink(content)) {
            message.setMessageType(ChatMessage.MessageType.LINK);
        } else {
            message.setMessageType(ChatMessage.MessageType.TEXT);
        }

        ChatMessage saved = chatMessageRepository.save(message);

        

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getConversation(User user1, User user2) {
        return chatMessageRepository.findConversation(user1, user2);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getRecentConversation(User user1, User user2) {
        return chatMessageRepository.findRecentConversation(user1, user2);
    }

    @Transactional(readOnly = true)
    public long countUnread(User user) {
        return chatMessageRepository.countUnreadMessages(user);
    }

    @Transactional(readOnly = true)
    public List<User> getConversationPartners(User user) {
        return chatMessageRepository.findConversationPartners(user.getId()); // Pass ID instead of object
    }

    public void markMessagesRead(User sender, User recipient) {
        List<ChatMessage> unread = chatMessageRepository.findByRecipientAndIsReadFalse(recipient);
        unread.stream()
            .filter(m -> m.getSender().getId().equals(sender.getId()))
            .forEach(m -> {
                m.setIsRead(true);
                m.setReadAt(LocalDateTime.now());
                chatMessageRepository.save(m);
            });
    }

    private String sanitize(String content) {
        if (content == null) return "";
        // Strip HTML tags — allow text and links only
        return content.replaceAll("<[^>]*>", "").trim();
    }

    private boolean isLink(String content) {
        if (content == null) return false;
        String lower = content.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.");
    }
    
    public List<ChatMessage> getGroupMessages(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    @Transactional
    public ChatMessage sendGroupMessage(User sender, String roomId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setRecipient(null); // Null recipient means group!
        msg.setRoomId(roomId);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        msg.setMessageType(ChatMessage.MessageType.TEXT);
        return chatMessageRepository.save(msg);
    }
}
