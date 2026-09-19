package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.service.NotificationService;
import com.threespace.progtrack.entity.Notification;
import com.threespace.progtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Controller
public class AuthController {
	
	@Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    // Define the upload directory (saves to an "uploads" folder in your project root)
    public static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads";

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                             @RequestParam(value = "logout", required = false) String logout,
                             @RequestParam(value = "expired", required = false) String expired,
                             Model model) {
        if (error != null) model.addAttribute("errorMsg", error);
        if (logout != null) model.addAttribute("logoutMsg", "You have been logged out successfully.");
        if (expired != null) model.addAttribute("expiredMsg", "Your session has expired. Please log in again.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", User.Role.values());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @ModelAttribute User user,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage, // The new image field
            RedirectAttributes redirectAttributes) {
        try {
            if (user.getRole() == null) user.setRole(User.Role.INTERN);

            // --- PROFILE PICTURE UPLOAD LOGIC ---
            if (profileImage != null && !profileImage.isEmpty()) {
                // 1. Create the uploads directory if it doesn't exist yet
                Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // 2. Generate a safe, unique filename (e.g., uuid-1234.jpg)
                String originalFilename = profileImage.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

                // 3. Save the file to the local folder
                Path filePath = uploadPath.resolve(uniqueFilename);
                Files.copy(profileImage.getInputStream(), filePath);

                // 4. Attach the filename to the user entity before saving
                user.setProfilePicture(uniqueFilename);
            }
            
            
            
         // ADD THIS: Explicitly mark the new user as unapproved
            user.setIsApproved(false);
            
            // ------------------------------------

            userService.createUser(user);
            
            
         // 2. 🚨 SEND NOTIFICATION TO ALL ADMINS 🚨
            // We find all active admins and send them an alert to review the new user
            List<User> admins = userService.getAllActiveUsers().stream()
                    .filter(u -> User.Role.ADMIN.equals(u.getRole()))
                    .toList();

            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        "New Registration: " + user.getFullName() + " has requested account access.",
                        Notification.NotificationType.SYSTEM_ALERT, // The new enum we added
                        "/admin/users/pending" // The actionUrl points straight to the approval page
                );
            }
            
            redirectAttributes.addFlashAttribute("successMsg", "Registration submitted successfully! Your account is pending Administrator approval.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }
}