package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.ChatMessage;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.service.ChatService;
import com.threespace.progtrack.service.NotificationService;
import com.threespace.progtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/chat")
public class ChatController {

    @Autowired private ChatService chatService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    /** Chat home – shows contact list, no active conversation */
    @GetMapping({"", "/"})
    @Transactional(readOnly = true)
    public String chatHome(@AuthenticationPrincipal UserDetails principal, Model model) {
        User me = userService.getByEmail(principal.getUsername());
        populateModel(model, me, null, null);
        model.addAttribute("pageTitle", "Team Chat");
        return "chat/chat";
    }

    /** Open a private conversation with another user */
    @GetMapping("/conversation/{userId}")
    @Transactional
    public String conversation(@PathVariable Long userId,
                               @AuthenticationPrincipal UserDetails principal,
                               Model model) {
        User me    = userService.getByEmail(principal.getUsername());
        User other = userService.getById(userId);
        
        chatService.markMessagesRead(other, me);
        List<ChatMessage> messages = chatService.getConversation(me, other);
        
        // CRITICAL FIX: Force Hibernate to load the lazy sender/recipient properties
        // before the database session closes so the HTML doesn't throw a 500 error.
        if (messages != null) {
            for (ChatMessage msg : messages) {
                if (msg.getSender() != null) msg.getSender().getFullName();
                if (msg.getRecipient() != null) msg.getRecipient().getFullName();
            }
        }

        populateModel(model, me, other, messages);
        model.addAttribute("pageTitle", "Chat — " + other.getFullName());
        return "chat/chat";
    }

    /** Open a Role-Based Broadcast Channel */
    @GetMapping("/group/{roomId}")
    @Transactional
    public String groupConversation(@PathVariable String roomId,
                                    @AuthenticationPrincipal UserDetails principal,
                                    Model model) {
        User me = userService.getByEmail(principal.getUsername());
        
        List<ChatMessage> messages = chatService.getGroupMessages(roomId);
        
        // Force lazy loading for UI
        if (messages != null) {
            for (ChatMessage msg : messages) {
                if (msg.getSender() != null) msg.getSender().getFullName();
            }
        }

        populateModel(model, me, null, messages);
        model.addAttribute("activeRoomId", roomId);
        
        // Set Friendly Names
        String roomName = roomId.equals("ANNOUNCEMENTS") ? "Company Announcements" : 
                          roomId.equals("MANAGEMENT") ? "Management Sync" : "Team Broadcasts";
        
        model.addAttribute("roomName", roomName);
        model.addAttribute("pageTitle", "Channel — " + roomName);
        return "chat/chat";
    }
    
    
    /** AJAX: unread message count for header badge */
    @GetMapping("/api/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails principal) {
        User me = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(Map.of("count", chatService.countUnread(me)));
    }

    private void populateModel(Model model, User me, User other, List<ChatMessage> messages) {
        List<User> contacts = userService.getAllActiveUsers();
        contacts.removeIf(u -> u.getId().equals(me.getId()));
        List<User> recent = chatService.getConversationPartners(me);
        long unread = chatService.countUnread(me);
        
        model.addAttribute("user",            me);
        model.addAttribute("otherUser",       other);
        model.addAttribute("messages",        messages);
        model.addAttribute("contacts",        contacts);
        model.addAttribute("recentContacts",  recent);
        model.addAttribute("unreadCount",     unread);
        model.addAttribute("chatUnread",      unread);
        model.addAttribute("notifications",   notificationService.getTop10(me));
        model.addAttribute("unreadNotifCount",notificationService.countUnseen(me));
    }
}