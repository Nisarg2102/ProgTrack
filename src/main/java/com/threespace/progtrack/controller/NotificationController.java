package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.Notification;
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
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired private NotificationService notificationService;
    @Autowired private UserService userService;
    @Autowired private ChatService chatService;

    @GetMapping({"", "/"})
    @Transactional(readOnly = true)
    public String notificationsPage(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.getByEmail(principal.getUsername());

        List<Notification> allNotifications = notificationService.getUnseenNotifications(user);
        long unseenCount = notificationService.countUnseen(user);

        model.addAttribute("user", user); 
        model.addAttribute("allNotifications", allNotifications);
        model.addAttribute("unseenCount", unseenCount);
        model.addAttribute("pageTitle", "Notifications");

        model.addAttribute("notifications", notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount", unseenCount);
        model.addAttribute("chatUnread", chatService.countUnread(user));

        return "notifications/notifications";
    }

    @GetMapping("/mark-all-read")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markAllRead(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.getByEmail(principal.getUsername());
        notificationService.markAllSeen(user);
        return ResponseEntity.ok(Map.of("status", "all_seen"));
    }

    @PostMapping("/mark-read/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markOneSeen(@PathVariable Long id) {
        notificationService.markSeen(id);
        return ResponseEntity.ok(Map.of("status", "seen"));
    }

    @GetMapping("/api/count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnseenCount(
            @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(Map.of("count", notificationService.countUnseen(user)));
    }
    
    // 🟢 BULLETPROOF REDIRECT HANDLER
    @GetMapping("/click/{id}")
    public String handleNotificationClick(@PathVariable Long id) {
    	// 1. Get the notification
        Notification notification = notificationService.getById(id);
        
        // 2. Mark it as read (This removes it from your unseen dropdown)
        notificationService.markSeen(id);
        
        // 3. Redirect to the target page safely
        String actionUrl = notification.getActionUrl();
        if (actionUrl != null && !actionUrl.trim().isEmpty() && !actionUrl.equals("#") && !actionUrl.equals("null")) {
            return "redirect:" + actionUrl;
        }
        
        // Fallback
        return "redirect:/notifications";
    }
}