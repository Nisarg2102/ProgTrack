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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/lead/reports")
@PreAuthorize("hasAnyRole('LEAD', 'CEO', 'ADMIN')")
public class LeadReportController {

    @Autowired private ReportSubmissionService reportSubmissionService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository; // Added to fix the 500 Error
    @Autowired private NotificationService notificationService;

    // ==========================================
    // WEEKLY REPORTS (Lead View)
    // ==========================================
    @GetMapping("/weekly")
    public String viewTeamWeeklyReports(@AuthenticationPrincipal UserDetails principal, Model model) {
        User lead = userService.getByEmail(principal.getUsername());
        String dept = lead.getDepartment();

        List<ReportSubmission> reports = reportSubmissionService.getReportsByDepartmentAndType(dept, ReportSubmission.ReportType.WEEKLY);
        Map<String, Object> stats = reportSubmissionService.getLeadReportStats(dept, ReportSubmission.ReportType.WEEKLY);

        model.addAttribute("user", lead);
        model.addAttribute("reports", reports);
        model.addAttribute("stats", stats);
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(lead));
        model.addAttribute("notifications", notificationService.getTop10(lead));
        model.addAttribute("parentPage", "Team");
        model.addAttribute("pageTitle", "Team Weekly Reports");
        
        List<User> teamMembers = userRepository.findActiveUsersByDepartment(dept); 
        model.addAttribute("teamMembers", teamMembers);

        return "lead/lead-weekly-reports";
    }

    @PostMapping("/weekly/review")
    public String reviewWeeklyReport(@AuthenticationPrincipal UserDetails principal,
                                     @RequestParam("reportId") Long reportId,
                                     @RequestParam("feedback") String feedback,
                                     RedirectAttributes redirectAttributes) {
        User lead = userService.getByEmail(principal.getUsername());
        try {
            reportSubmissionService.reviewReport(reportId, lead, feedback);
            redirectAttributes.addFlashAttribute("successMsg", "Weekly report reviewed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error saving review: " + e.getMessage());
        }
        return "redirect:/lead/reports/weekly";
    }

    // ==========================================
    // MONTHLY REPORTS (Lead View)
    // ==========================================
    @GetMapping("/monthly")
    public String viewTeamMonthlyReports(@AuthenticationPrincipal UserDetails principal, Model model) {
        User lead = userService.getByEmail(principal.getUsername());
        String dept = lead.getDepartment();

        List<ReportSubmission> reports = reportSubmissionService.getReportsByDepartmentAndType(dept, ReportSubmission.ReportType.MONTHLY);
        Map<String, Object> stats = reportSubmissionService.getLeadReportStats(dept, ReportSubmission.ReportType.MONTHLY);

        model.addAttribute("user", lead);
        model.addAttribute("reports", reports);
        model.addAttribute("stats", stats);
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(lead));
        model.addAttribute("notifications", notificationService.getTop10(lead));
        model.addAttribute("parentPage", "Team");
        model.addAttribute("pageTitle", "Team Monthly Reports");
        
        List<User> teamMembers = userRepository.findActiveUsersByDepartment(dept); 
        model.addAttribute("teamMembers", teamMembers);

        return "lead/lead-monthly-reports";
    }

    @PostMapping("/monthly/review")
    public String reviewMonthlyReport(@AuthenticationPrincipal UserDetails principal,
                                      @RequestParam("reportId") Long reportId,
                                      @RequestParam("feedback") String feedback,
                                      RedirectAttributes redirectAttributes) {
        User lead = userService.getByEmail(principal.getUsername());
        try {
            reportSubmissionService.reviewReport(reportId, lead, feedback);
            redirectAttributes.addFlashAttribute("successMsg", "Monthly report reviewed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error saving review: " + e.getMessage());
        }
        return "redirect:/lead/reports/monthly";
    }
}