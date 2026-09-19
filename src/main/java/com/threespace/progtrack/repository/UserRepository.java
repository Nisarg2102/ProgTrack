package com.threespace.progtrack.repository;

import com.threespace.progtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByDepartment(String department);

    List<User> findByRole(User.Role role);

    List<User> findByDepartmentAndRole(String department, User.Role role);

    List<User> findByIsActiveTrue();

    List<User> findByProjectGroup(String projectGroup);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    List<User> findActiveUsersByRole(@Param("role") User.Role role);

    @Query("SELECT u FROM User u WHERE u.department = :dept AND u.isActive = true ORDER BY u.fullName")
    List<User> findActiveUsersByDepartment(@Param("dept") String department);

    @Query("SELECT DISTINCT u.department FROM User u WHERE u.department IS NOT NULL")
    List<String> findAllDepartments();

    @Query("SELECT DISTINCT u.projectGroup FROM User u WHERE u.projectGroup IS NOT NULL")
    List<String> findAllProjectGroups();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isActive = true")
    long countActiveUsersByRole(@Param("role") User.Role role);
    
    List<User> findByRoleAndIsActiveTrue(User.Role role);
    
    List<User> findByIsApprovedFalse();
}
