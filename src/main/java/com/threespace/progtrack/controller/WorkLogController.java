package com.threespace.progtrack.controller;

import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.entity.WorkLog;
import com.threespace.progtrack.service.NotificationService;
import com.threespace.progtrack.service.TaskService;
import com.threespace.progtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/worklog")
public class WorkLogController {

    @Autowired private TaskService taskService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    @GetMapping({"", "/"})
    public String myWorkLog(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        User user = userService.getByEmail(principal.getUsername());

        LocalDate endDate   = (to   != null) ? to   : LocalDate.now();
        LocalDate startDate = (from != null) ? from : endDate.minusDays(29);

        List<WorkLog> workLogs = taskService.getWorkLogsByUserAndDateRange(user, startDate, endDate);

        model.addAttribute("user",            user);
        model.addAttribute("workLogs",        workLogs);
        model.addAttribute("from",            startDate);
        model.addAttribute("to",              endDate);
        model.addAttribute("notifications",   notificationService.getTop10(user));
        model.addAttribute("unreadNotifCount",notificationService.countUnseen(user));
        model.addAttribute("pageTitle",       "My Work Log");

        return "profile/worklog";
    }
}
