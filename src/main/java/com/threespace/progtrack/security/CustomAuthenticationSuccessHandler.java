package com.threespace.progtrack.security;

import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.UserRepository;
import com.threespace.progtrack.repository.WorkLogRepository;
import com.threespace.progtrack.entity.WorkLog;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkLogRepository workLogRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String email = authentication.getName();
        HttpSession session = request.getSession();
        String sessionId = session.getId();
        LocalDateTime loginTime = LocalDateTime.now();

        // ── 1. Update user's currentLoginTime ──────────────────────────
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setCurrentLoginTime(loginTime);
            userRepository.save(user);

            // ── 2. Create WorkLog entry for this session ───────────────
            WorkLog workLog = new WorkLog();
            workLog.setUser(user);
            workLog.setLoginDateTime(loginTime);
            workLog.setSessionId(sessionId);

            // Capture client IP
            String ipAddress = getClientIpAddress(request);
            workLog.setIpAddress(ipAddress);
            workLogRepository.save(workLog);

            // Store workLog ID in session for logout pairing
            session.setAttribute("WORKLOG_ID", workLog.getId());
            session.setAttribute("LOGIN_TIME", loginTime);
            session.setAttribute("USER_EMAIL", email);
            session.setAttribute("USER_ROLE", user.getRole().name());
            session.setAttribute("USER_NAME", user.getFullName());
        });

        // ── 3. Role-based redirect ─────────────────────────────────────
        String redirectUrl = determineTargetUrl(authentication);
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            switch (role) {
                case "ROLE_ADMIN":
                    return "/admin/dashboard";
                case "ROLE_CEO":
                    return "/dashboard/executive";
                case "ROLE_LEAD":
                    return "/dashboard/lead";
                case "ROLE_INTERN":
                    return "/dashboard/intern";
                default:
                    return "/dashboard";
            }
        }
        return "/dashboard";
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
