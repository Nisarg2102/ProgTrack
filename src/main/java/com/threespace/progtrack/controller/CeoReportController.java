package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.ReportSubmission;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.UserRepository;
import com.threespace.progtrack.service.NotificationService;
import com.threespace.progtrack.service.ReportSubmissionService;
import com.threespace.progtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ceo/reports")
@PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
public class CeoReportController {

    @Autowired private ReportSubmissionService reportSubmissionService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    // ==========================================
    // COMPANY MONTHLY REPORTS (CEO View)
    // ==========================================
    @GetMapping("/monthly")
    @Transactional(readOnly = true)
    public String viewCompanyMonthlyReports(@AuthenticationPrincipal UserDetails principal, Model model) {
        User ceo = userService.getByEmail(principal.getUsername());

        // Fetch ALL reports across the entire company
        List<ReportSubmission> reports = reportSubmissionService.getAllReportsByType(ReportSubmission.ReportType.MONTHLY);
        
        // Fetch ALL interns across the entire company for the filter dropdown
        List<User> teamMembers = userRepository.findByRoleAndIsActiveTrue(User.Role.INTERN);

        // Fetch company-wide stats for the KPIs
        Map<String, Object> stats = reportSubmissionService.getCompanyReportStats(ReportSubmission.ReportType.MONTHLY);

        model.addAttribute("user", ceo);
        model.addAttribute("reports", reports);
        model.addAttribute("stats", stats);
        model.addAttribute("teamMembers", teamMembers);
        
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(ceo));
        model.addAttribute("notifications", notificationService.getTop10(ceo));
        model.addAttribute("parentPage", "Company");
        model.addAttribute("pageTitle", "All Monthly Reports");

        // Make sure you save the HTML file from the previous step in your templates folder 
        // with this exact name and path (e.g., templates/ceo-monthly-reports.html)
        return "ceo/ceo-monthly-reports"; 
    }

    @PostMapping("/monthly/review")
    public String reviewCompanyMonthlyReport(@AuthenticationPrincipal UserDetails principal,
                                      @RequestParam("reportId") Long reportId,
                                      @RequestParam("feedback") String feedback,
                                      RedirectAttributes redirectAttributes) {
        User ceo = userService.getByEmail(principal.getUsername());
        try {
            reportSubmissionService.reviewReport(reportId, ceo, feedback);
            redirectAttributes.addFlashAttribute("successMsg", "Monthly report reviewed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error saving review: " + e.getMessage());
        }
        return "redirect:/ceo/reports/monthly";
    }
}