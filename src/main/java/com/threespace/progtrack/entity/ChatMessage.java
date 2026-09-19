package com.threespace.progtrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient; // null means broadcast/group message

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Message content cannot be empty")
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "room_id", length = 100)
    private String roomId; // for group chat rooms

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public enum MessageType {
        TEXT, LINK, NOTIFICATION, SYSTEM
    }

    // =====================
    // Constructors
    // =====================

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(User sender, User recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.messageType = MessageType.TEXT;
    }

    public ChatMessage(User sender, User recipient, String content, MessageType messageType) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.messageType = messageType;
    }

    // =====================
    // Getters & Setters
    // =====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    // =====================
    // toString
    // =====================

    @Override
    public String toString() {
        return "ChatMessage{" +
               "id=" + id +
               ", senderId=" + (sender != null ? sender.getId() : null) +
               ", recipientId=" + (recipient != null ? recipient.getId() : null) +
               ", content='" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
               ", timestamp=" + timestamp +
               ", isRead=" + isRead +
               '}';
    }
}
