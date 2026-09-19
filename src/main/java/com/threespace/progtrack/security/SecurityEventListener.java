package com.threespace.progtrack.security;

import com.threespace.progtrack.entity.WorkLog;
import com.threespace.progtrack.repository.WorkLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Listens to Spring Security authentication and session lifecycle events.
 * Handles auto-timer: records loginDateTime on success,
 * and logoutDateTime + totalSeconds on SessionDestroyedEvent.
 */
@Component
public class SecurityEventListener {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventListener.class);

    @Autowired
    private WorkLogRepository workLogRepository;

    // =====================
    // LOGIN SUCCESS
    // =====================

    @EventListener
    public void handleAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String email = "";
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }
        log.info("✅ LOGIN SUCCESS — User: {}", email);
        // WorkLog creation is handled in CustomAuthenticationSuccessHandler
        // to get the session ID and IP address
    }

    // =====================
    // SESSION DESTROYED (Logout / Timeout)
    // =====================

    @EventListener
    @Transactional
    public void handleSessionDestroyed(SessionDestroyedEvent event) {
        String sessionId = event.getId();
        log.info("🔒 SESSION DESTROYED — Session ID: {}", sessionId);

        LocalDateTime logoutTime = LocalDateTime.now();

        // Find the WorkLog for this session
        Optional<WorkLog> workLogOpt = workLogRepository.findBySessionIdAndLogoutDateTimeIsNull(sessionId);

        workLogOpt.ifPresentOrElse(workLog -> {
            workLog.setLogoutDateTime(logoutTime);
            workLog.calculateTotalSeconds();
            workLogRepository.save(workLog);

            log.info("⏱️ WorkLog updated — User: {}, Duration: {}",
                workLog.getUser() != null ? workLog.getUser().getEmail() : "unknown",
                workLog.getFormattedDuration());
        }, () -> {
            log.warn("⚠️ No open WorkLog found for session: {}", sessionId);
        });
    }

    // =====================
    // LOGIN FAILURE
    // =====================

    @EventListener
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        log.warn("❌ LOGIN FAILED — Attempted username: {}", principal);
    }
}
