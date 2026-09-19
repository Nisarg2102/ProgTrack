package com.threespace.progtrack.service;

import com.threespace.progtrack.entity.Notification;
import com.threespace.progtrack.entity.ReportSubmission;
import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.ReportSubmissionRepository;
import com.threespace.progtrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ReportSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(ReportSubmissionService.class);
    
    // Change this to your actual upload directory path
    private final String UPLOAD_DIR = "uploads/reports/";

    @Autowired
    private ReportSubmissionRepository reportSubmissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    // =====================
    // SUBMIT REPORT
    // =====================
    public ReportSubmission submitReport(User intern, ReportSubmission.ReportType type, String period, String comments, MultipartFile file) throws IOException {
        
        // 1. Save the File
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;
        
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 2. Create and Save Entity
        ReportSubmission submission = new ReportSubmission();
        submission.setUser(intern);
        submission.setType(type);
        submission.setPeriod(period);
        submission.setComments(comments);
        submission.setFileName(uniqueFileName);
        submission.setStatus(ReportSubmission.ReportStatus.PENDING);
        
        ReportSubmission saved = reportSubmissionRepository.save(submission);

        
     // 🟢 FIX: Generate the CORRECT URL for the Lead based on report type
        String leadActionUrl = (type == ReportSubmission.ReportType.WEEKLY) 
                ? "/lead/reports/weekly" 
                : "/lead/reports/monthly";
        
        // 3. Notify the Lead(s) of that specific department
        userRepository.findActiveUsersByDepartment(intern.getDepartment())
            .stream()
            .filter(u -> User.Role.LEAD.equals(u.getRole()))
            .forEach(lead -> notificationService.createNotification(
                lead,
                intern.getFullName() + " submitted their " + type.name() + " report for " + period + ".",
                Notification.NotificationType.REPORT_SUBMITTED, // Or create a new enum type like REPORT_SUBMITTED
                leadActionUrl
            ));

        log.info("📄 {} report submitted by {} for period {}", type, intern.getEmail(), period);
        return saved;
    }

    // =====================
    // FETCH REPORTS
    // =====================
    @Transactional(readOnly = true)
    public List<ReportSubmission> getInternHistory(User intern, ReportSubmission.ReportType type) {
        return reportSubmissionRepository.findByUserAndTypeOrderBySubmittedAtDesc(intern, type);
    }

    // =====================
    // REVIEW REPORT (Lead Action)
    // =====================
    @Transactional
    public void reviewReport(Long reportId, User reviewer, String feedback) {
        // 1. Find the report
        ReportSubmission report = reportSubmissionRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        // 2. Update status and feedback
        report.setStatus(ReportSubmission.ReportStatus.REVIEWED);
        report.setFeedback(feedback);

        // 3. SET THE REVIEWER ROLE (This fixes the HTML Badge!)
        String roleName = reviewer.getRole().name(); // Will be "LEAD" or "CEO"
        report.setReviewerRole(roleName);

        reportSubmissionRepository.save(report);

        // 4. GENERATE THE ACTION URL FOR THE REDIRECT
        String actionUrl = report.getType() == ReportSubmission.ReportType.WEEKLY 
                ? "/workspace/reports/weekly" 
                : "/workspace/reports/monthly";

        // 5. CREATE DYNAMIC MESSAGE
        String reportTypeName = report.getType() == ReportSubmission.ReportType.WEEKLY ? "Weekly" : "Monthly";
        String message = "Your " + reportTypeName + " Report was reviewed by " + roleName;

        // 6. SEND NOTIFICATION (This fixes the redirect!)
        notificationService.createNotification(
                report.getUser(), 
                message, 
                Notification.NotificationType.REPORT_REVIEWED, 
                actionUrl // <--- This URL makes the redirect work!
        );
    }
    
 // =====================
    // LEAD KPI STATS
    // =====================
    @Transactional(readOnly = true)
    public Map<String, Object> getLeadReportStats(String department, ReportSubmission.ReportType type) {
        List<ReportSubmission> reports = reportSubmissionRepository.findByDepartmentAndType(department, type);
        
        long total = reports.size();
        long pending = reports.stream().filter(r -> r.getStatus() == ReportSubmission.ReportStatus.PENDING).count();
        long reviewed = reports.stream().filter(r -> r.getStatus() == ReportSubmission.ReportStatus.REVIEWED).count();

        Map<String, Object> stats = new java.util.HashMap<>();
        String suffix = (type == ReportSubmission.ReportType.WEEKLY) ? "Weekly" : "Monthly";
        
        stats.put("total" + suffix, total);
        stats.put("pending" + suffix, pending);
        stats.put("reviewed" + suffix, reviewed);
        
        return stats;
    }
    
    // Quick helper to just get the list
    @Transactional(readOnly = true)
    public List<ReportSubmission> getReportsByDepartmentAndType(String department, ReportSubmission.ReportType type) {
        return reportSubmissionRepository.findByDepartmentAndType(department, type);
    }
    
 // Add this to fetch ALL reports regardless of department
    public List<ReportSubmission> getAllReportsByType(ReportSubmission.ReportType type) {
        return reportSubmissionRepository.findByTypeOrderBySubmittedAtDesc(type);
    }

    // Add this to calculate KPI stats for the entire company
    public Map<String, Object> getCompanyReportStats(ReportSubmission.ReportType type) {
        List<ReportSubmission> allReports = getAllReportsByType(type);
        
        long total = allReports.size();
        long pending = allReports.stream()
                .filter(r -> r.getStatus() == ReportSubmission.ReportStatus.PENDING)
                .count();
        long reviewed = allReports.stream()
                .filter(r -> r.getStatus() == ReportSubmission.ReportStatus.REVIEWED)
                .count();

        Map<String, Object> stats = new HashMap<>();
        
        // Dynamic keys so it works for both Weekly and Monthly if you expand it later
        String prefix = (type == ReportSubmission.ReportType.WEEKLY) ? "Weekly" : "Monthly";
        stats.put("total" + prefix, total);
        stats.put("pending" + prefix, pending);
        stats.put("reviewed" + prefix, reviewed);
        
        return stats;
    }
    
}