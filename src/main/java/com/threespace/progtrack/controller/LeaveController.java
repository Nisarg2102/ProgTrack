package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.LeaveRequest;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.service.LeaveService;
import com.threespace.progtrack.service.NotificationService;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/leaves")
public class LeaveController {

    @Autowired private LeaveService leaveService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    // ─── My Leaves ─────────────────────────────────────────────────────────────
    @GetMapping("/my")
    @PreAuthorize("!hasAnyRole('CEO', 'ADMIN')") // FIX: Block CEO and Admin
    public String myLeaves(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.getByEmail(principal.getUsername());
        List<LeaveRequest> leaves = leaveService.getLeaveRequestsForUser(user);

        long pendingCount  = leaves.stream().filter(l -> LeaveRequest.LeaveStatus.PENDING.equals(l.getStatus())).count();
        long approvedCount = leaves.stream().filter(l -> LeaveRequest.LeaveStatus.APPROVED.equals(l.getStatus())).count();
        long rejectedCount = leaves.stream().filter(l -> LeaveRequest.LeaveStatus.REJECTED.equals(l.getStatus())).count();

        model.addAttribute("user",              user);
        model.addAttribute("leaves",            leaves);
        model.addAttribute("leaveTypes",        LeaveRequest.LeaveType.values());
        model.addAttribute("pendingCount",      pendingCount);
        model.addAttribute("approvedCount",     approvedCount);
        model.addAttribute("rejectedCount",     rejectedCount);
        model.addAttribute("notifications",     notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(user));
        model.addAttribute("pageTitle",         "My Leaves");
        return "leave/my-leaves";
    }

    // ─── Apply Leave ───────────────────────────────────────────────────────────
    @PostMapping("/apply")
    @PreAuthorize("!hasAnyRole('CEO', 'ADMIN')") // FIX: Block CEO and Admin
    public String applyLeave(@AuthenticationPrincipal UserDetails principal,
                             @ModelAttribute LeaveRequest leaveRequest,
                             RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getUsername());
        try {
            leaveService.applyForLeave(user, leaveRequest);
            redirectAttributes.addFlashAttribute("successMsg",
                    "✅ Leave request submitted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "❌ " + e.getMessage());
        }
        return "redirect:/leaves/my";
    }

    // ─── Cancel Leave ──────────────────────────────────────────────────────────
    @PostMapping("/cancel/{id}")
    @PreAuthorize("!hasAnyRole('CEO', 'ADMIN')") // FIX: Block CEO and Admin
    public String cancelLeave(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails principal,
                              RedirectAttributes redirectAttributes) {
        User user = userService.getByEmail(principal.getUsername());
        try {
            leaveService.cancelLeave(id, user);
            redirectAttributes.addFlashAttribute("successMsg", "Leave request cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "❌ " + e.getMessage());
        }
        return "redirect:/leaves/my";
    }

    // ─── Lead: manage department leaves ────────────────────────────────────
    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('LEAD','CEO','ADMIN')")
    public String manageLeaves(@AuthenticationPrincipal UserDetails principal, Model model) {
        User lead = userService.getByEmail(principal.getUsername());

        List<LeaveRequest> leaves;
        
        // FIX: Implement Hierarchy View Logic
        if (lead.getRole() == User.Role.LEAD) {
            // Leads only see Interns from their department
            leaves = leaveService.getDepartmentLeaveRequests(lead.getDepartment())
                    .stream()
                    .filter(l -> l.getUser().getRole() == User.Role.INTERN)
                    .collect(Collectors.toList());
                    
        } else if (lead.getRole() == User.Role.CEO) {
            // CEO only sees Leads
            leaves = leaveService.getAllLeaveRequests()
                    .stream()
                    .filter(l -> l.getUser().getRole() == User.Role.LEAD)
                    .collect(Collectors.toList());
                    
        } else {
            // ADMIN sees everything
            leaves = leaveService.getAllLeaveRequests();
        }

        long pendingCount  = leaves.stream().filter(l -> LeaveRequest.LeaveStatus.PENDING.equals(l.getStatus())).count();
        long approvedCount = leaves.stream().filter(l -> LeaveRequest.LeaveStatus.APPROVED.equals(l.getStatus())).count();
        long rejectedCount = leaves.stream().filter(l -> LeaveRequest.LeaveStatus.REJECTED.equals(l.getStatus())).count();

        
        String parentPageName = (lead.getRole() == User.Role.CEO || lead.getRole() == User.Role.ADMIN) 
                ? "Company" 
                : "Team";
        
        model.addAttribute("user",              lead);
        model.addAttribute("leaves",            leaves);
        model.addAttribute("pendingCount",      pendingCount);
        model.addAttribute("approvedCount",     approvedCount);
        model.addAttribute("rejectedCount",     rejectedCount);
        model.addAttribute("notifications",     notificationService.getTop10(lead));
        model.addAttribute("unreadNotifCount", notificationService.countUnseen(lead));
        model.addAttribute("parentPage",        parentPageName);
        model.addAttribute("pageTitle",         "Leave Management");
        return "leave/manage";
    }

    // ─── Approve ──────────────────────────────────────────────────────────────
    @PostMapping("/approve/{id}")
    @PreAuthorize("hasAnyRole('LEAD','CEO','ADMIN')")
    public String approveLeave(@PathVariable Long id,
                               @RequestParam(required = false) String comment,
                               @AuthenticationPrincipal UserDetails principal,
                               RedirectAttributes redirectAttributes) {
        try {
            leaveService.approveLeave(id, principal.getUsername(), comment);
            redirectAttributes.addFlashAttribute("successMsg", "✅ Leave approved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "❌ " + e.getMessage());
        }
        return "redirect:/leaves/manage";
    }

    // ─── Reject ───────────────────────────────────────────────────────────────
    @PostMapping("/reject/{id}")
    @PreAuthorize("hasAnyRole('LEAD','CEO','ADMIN')")
    public String rejectLeave(@PathVariable Long id,
                              @RequestParam(required = false) String comment,
                              @AuthenticationPrincipal UserDetails principal,
                              RedirectAttributes redirectAttributes) {
        try {
            leaveService.rejectLeave(id, principal.getUsername(), comment);
            redirectAttributes.addFlashAttribute("successMsg", "Leave rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "❌ " + e.getMessage());
        }
        return "redirect:/leaves/manage";
    }
}