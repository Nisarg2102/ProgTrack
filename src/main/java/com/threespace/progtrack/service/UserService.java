package com.threespace.progtrack.service;

import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    // =====================
    // CREATE
    // =====================

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + user.getEmail());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsActive(true);
        User saved = userRepository.save(user);
        log.info("🆕 User created: {} [{}]", saved.getEmail(), saved.getRole());

        // Welcome notification
        notificationService.createNotification(
            saved,
            "Welcome to ProgTrack, " + saved.getFullName() + "! Your account is ready.",
            com.threespace.progtrack.entity.Notification.NotificationType.SYSTEM
        );

        return saved;
    }

    // =====================
    // READ
    // =====================

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByDepartment(String department) {
        return userRepository.findActiveUsersByDepartment(department);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findActiveUsersByRole(role);
    }

    @Transactional(readOnly = true)
    public List<String> getAllDepartments() {
        return userRepository.findAllDepartments();
    }

    @Transactional(readOnly = true)
    public List<String> getAllProjectGroups() {
        return userRepository.findAllProjectGroups();
    }

    // =====================
    // UPDATE
    // =====================

    public User updateUser(User user) {
        User existing = getById(user.getId());
        existing.setFullName(user.getFullName());
        existing.setDesignation(user.getDesignation());
        existing.setDepartment(user.getDepartment());
        existing.setProjectGroup(user.getProjectGroup());
        existing.setPhoneNumber(user.getPhoneNumber());
        // Role and email changes are admin-only
        return userRepository.save(existing);
    }

    public User updateProfileByAdmin(User user) {
        User existing = getById(user.getId());
        existing.setFullName(user.getFullName());
        existing.setDesignation(user.getDesignation());
        existing.setDepartment(user.getDepartment());
        existing.setProjectGroup(user.getProjectGroup());
        existing.setRole(user.getRole());
        existing.setIsActive(user.getIsActive());
        existing.setPhoneNumber(user.getPhoneNumber());
        return userRepository.save(existing);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("🔑 Password changed for user: {}", user.getEmail());
    }

    public void resetPassword(Long userId, String newPassword) {
        User user = getById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("🔑 Password reset by admin for user: {}", user.getEmail());
    }

    // =====================
    // ACTIVATE / DEACTIVATE
    // =====================

    public void deactivateUser(Long userId) {
        User user = getById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        log.info("🚫 User deactivated: {}", user.getEmail());
    }

    public void activateUser(Long userId) {
        User user = getById(userId);
        user.setIsActive(true);
        userRepository.save(user);
        log.info("✅ User activated: {}", user.getEmail());
    }
}
