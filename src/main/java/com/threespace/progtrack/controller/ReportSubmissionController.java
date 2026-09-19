package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.ReportSubmission;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.service.NotificationService;
import com.threespace.progtrack.service.ReportSubmissionService;
import com.threespace.progtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;

import java.util.List;

@Controller
@RequestMapping("/workspace/reports")
public class ReportSubmissionController {

    @Autowired private ReportSubmissionService reportSubmissionService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    // ==========================================
    // WEEKLY REPORTS (Intern View)
    // ==========================================
    @GetMapping("/weekly")
    @PreAuthorize("hasRole('INTERN')")
    public String weeklyReportPage(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.getByEmail(principal.getUsername());
        List<ReportSubmission> history = reportSubmissionService.getInternHistory(user, ReportSubmission.ReportType.WEEKLY);

        model.addAttribute("user", user);
        model.addAttribute("weeklyReports", history);
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(user));
        model.addAttribute("notifications", notificationService.getTop10(user));
        model.addAttribute("wordFileSize", getFileSize("Weekly_Template.docx"));
        model.addAttribute("pdfFileSize", getFileSize("Weekly_Guide.pdf"));
        model.addAttribute("parentPage",        "My Workspace");
        model.addAttribute("pageTitle", "Weekly Reports");
        
        return "workspace/weekly-report";
    }

    @PostMapping("/weekly/submit")
    @PreAuthorize("hasRole('INTERN')")
    public String submitWeeklyReport(@AuthenticationPrincipal UserDetails principal,
                                     @RequestParam("reportWeek") String reportWeek,
                                     @RequestParam("comments") String comments,
                                     @RequestParam("file") MultipartFile file,
                                     RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getUsername());
        try {
            reportSubmissionService.submitReport(user, ReportSubmission.ReportType.WEEKLY, reportWeek, comments, file);
            redirectAttributes.addFlashAttribute("successMsg", "Weekly report submitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to submit report: " + e.getMessage());
        }
        return "redirect:/workspace/reports/weekly";
    }

    // ==========================================
    // MONTHLY REPORTS (Intern View)
    // ==========================================
    @GetMapping("/monthly")
    @PreAuthorize("hasRole('INTERN')")
    public String monthlyReportPage(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.getByEmail(principal.getUsername());
        List<ReportSubmission> history = reportSubmissionService.getInternHistory(user, ReportSubmission.ReportType.MONTHLY);

        model.addAttribute("user", user);
        model.addAttribute("monthlyReports", history);
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(user));
        model.addAttribute("notifications", notificationService.getTop10(user));
        model.addAttribute("wordFileSize", getFileSize("Monthly_Template.docx"));
        model.addAttribute("pdfFileSize", getFileSize("Monthly_Guide.pdf"));
        model.addAttribute("parentPage",        "My Workspace");
        model.addAttribute("pageTitle", "Monthly Reports");
        
        return "workspace/monthly-report";
    }

    @PostMapping("/monthly/submit")
    @PreAuthorize("hasRole('INTERN')")
    public String submitMonthlyReport(@AuthenticationPrincipal UserDetails principal,
                                      @RequestParam("reportMonth") String reportMonth,
                                      @RequestParam("comments") String comments,
                                      @RequestParam("file") MultipartFile file,
                                      RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getUsername());
        try {
            reportSubmissionService.submitReport(user, ReportSubmission.ReportType.MONTHLY, reportMonth, comments, file);
            redirectAttributes.addFlashAttribute("successMsg", "Monthly report submitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to submit report: " + e.getMessage());
        }
        return "redirect:/workspace/reports/monthly";
    }
    
 // Helper method to get formatted file size
    private String getFileSize(String filePath) {
        try {
            ClassPathResource resource = new ClassPathResource("static/assets/templates/" + filePath);
            if (resource.exists()) {
                // FIX: Use contentLength() instead of getFile().length()
                // This is much safer and works even when the app is packaged as a JAR!
                long bytes = resource.contentLength(); 
                
                if (bytes < 1024) return bytes + " B";
                int kb = (int) (bytes / 1024);
                if (kb < 1024) return kb + " KB";
                return String.format("%.1f MB", kb / 1024.0);
            } else {
                System.out.println("File not found: " + filePath); // Helps you debug if the file is missing
            }
        } catch (IOException e) {
            System.out.println("Error reading file size for " + filePath + ": " + e.getMessage());
        }
        return "Unknown Size";
    }
}