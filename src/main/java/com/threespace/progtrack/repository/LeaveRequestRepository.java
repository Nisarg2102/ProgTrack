package com.threespace.progtrack.repository;

import com.threespace.progtrack.entity.LeaveRequest;
import com.threespace.progtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // FIX: JOIN FETCH l.user forces Hibernate to load the User in the same
    // SQL query instead of lazily. Without this, leave.user is a hollow proxy
    // that throws LazyInitializationException when Thymeleaf accesses it
    // after the transaction closes — causing ERR_INCOMPLETE_CHUNKED_ENCODING.

    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.user WHERE l.user = :user ORDER BY l.createdAt DESC")
    List<LeaveRequest> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.user WHERE l.status = :status ORDER BY l.createdAt DESC")
    List<LeaveRequest> findByStatus(@Param("status") LeaveRequest.LeaveStatus status);

    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.user WHERE l.user = :user AND l.status = :status ORDER BY l.createdAt DESC")
    List<LeaveRequest> findByUserAndStatus(@Param("user") User user,
                                           @Param("status") LeaveRequest.LeaveStatus status);

    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.user u WHERE u.department = :dept ORDER BY l.createdAt DESC")
    List<LeaveRequest> findByDepartment(@Param("dept") String department);

    // This is the one called by getPendingDepartmentLeaves — the critical fix
    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.user u WHERE u.department = :dept AND l.status = :status ORDER BY l.createdAt DESC")
    List<LeaveRequest> findByDepartmentAndStatus(@Param("dept") String department,
                                                  @Param("status") LeaveRequest.LeaveStatus status);

    @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.user = :user AND l.status = 'PENDING'")
    long countPendingByUser(@Param("user") User user);

    @Query("SELECT l FROM LeaveRequest l JOIN FETCH l.user ORDER BY l.createdAt DESC")
    List<LeaveRequest> findAllOrderByCreatedAtDesc();
}