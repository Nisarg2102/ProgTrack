package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.UserRepository; // <-- NEW IMPORT
import com.threespace.progtrack.service.NotificationService;
import com.threespace.progtrack.service.TaskService;
import com.threespace.progtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired private UserService userService;
    @Autowired private TaskService taskService;
    @Autowired private NotificationService notificationService;
    
    // CRITICAL FIX: Injecting the Repository to bypass the restrictive Service layer!
    @Autowired private UserRepository userRepository; 

    @GetMapping({"", "/"})
    public String viewProfile(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.getByEmail(principal.getUsername());

        model.addAttribute("user", user);
        model.addAttribute("notifications", notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(user));
        model.addAttribute("pageTitle", "My Profile");

        // Work stats for display
        long todaySec = taskService.calculateTodayOnlineSeconds(user);
        model.addAttribute("todayOnlineHours",
                Math.round(todaySec / 3600.0 * 100.0) / 100.0);
        model.addAttribute("formattedOnlineTime", taskService.formatSeconds(todaySec));

        return "profile/profile";
    }

    // ==========================================
    // Unified Update Method: Forces MySQL Save
    // ==========================================
    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails principal,
                                @ModelAttribute User formUser,
                                @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                                RedirectAttributes redirectAttributes) {
        try {
            // 1. Fetch the complete user from the database
            User existingUser = userService.getByEmail(principal.getUsername());
            
            // 2. Update text fields
            existingUser.setFullName(formUser.getFullName());
            existingUser.setDesignation(formUser.getDesignation());
            existingUser.setDepartment(formUser.getDepartment());
            existingUser.setProjectGroup(formUser.getProjectGroup());
            existingUser.setPhoneNumber(formUser.getPhoneNumber());

            // 3. Process the Image Upload if a new one was selected
            if (profileImage != null && !profileImage.isEmpty()) {
                Path uploadPath = Paths.get(AuthController.UPLOAD_DIRECTORY);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Generate a unique filename
                String originalFilename = profileImage.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

                // Save image file to local directory
                Path filePath = uploadPath.resolve(uniqueFilename);
                Files.copy(profileImage.getInputStream(), filePath);

                // Attach the NEW image filename to the entity
                existingUser.setProfilePicture(uniqueFilename);
            }

            // 4. THE FIX: Save directly using the Repository!
            // This forces Hibernate to write the profile_picture field to MySQL
            userRepository.save(existingUser);
            
            redirectAttributes.addFlashAttribute("successMsg", "✅ Profile updated successfully.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "❌ Update failed: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMsg", "❌ New passwords do not match. Please try again.");
            return "redirect:/profile";
        }
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMsg", "❌ Password must be at least 6 characters.");
            return "redirect:/profile";
        }
        try {
            User user = userService.getByEmail(principal.getUsername());
            userService.changePassword(user.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMsg", "✅ Password changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "❌ " + e.getMessage());
        }
        return "redirect:/profile";
    }
}