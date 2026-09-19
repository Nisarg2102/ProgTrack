package com.threespace.progtrack.repository;

import com.threespace.progtrack.entity.ReportSubmission;
import com.threespace.progtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportSubmissionRepository extends JpaRepository<ReportSubmission, Long> {

    // For the Intern: Get their history of a specific report type
    List<ReportSubmission> findByUserAndTypeOrderBySubmittedAtDesc(User user, ReportSubmission.ReportType type);

    // For the Lead: Get all pending reports for their specific department
    @Query("SELECT r FROM ReportSubmission r JOIN FETCH r.user u WHERE u.department = :department AND r.status = :status ORDER BY r.submittedAt DESC")
    List<ReportSubmission> findByDepartmentAndStatus(String department, ReportSubmission.ReportStatus status);

    // For CEO/Admin: Get all reports across the company
    List<ReportSubmission> findAllByOrderBySubmittedAtDesc();
    
 // Fetch all reports of a specific type (WEEKLY/MONTHLY) for a specific department
    @Query("SELECT r FROM ReportSubmission r JOIN FETCH r.user u WHERE u.department = :department AND r.type = :type ORDER BY r.submittedAt DESC")
    List<ReportSubmission> findByDepartmentAndType(String department, ReportSubmission.ReportType type);

    List<ReportSubmission> findByTypeOrderBySubmittedAtDesc(ReportSubmission.ReportType type);

}