package com.threespace.progtrack.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String errorMessage;

        if (exception instanceof BadCredentialsException) {
            errorMessage = "Incorrect email or password. Please verify your credentials and try again.";
        } else if (exception instanceof DisabledException) {
            // This will now dynamically grab the custom message we set in the UserDetailsService!
            errorMessage = exception.getMessage();
        } else if (exception instanceof LockedException) {
            errorMessage = "Your account is locked. Please contact your administrator.";
        } else {
            errorMessage = "Authentication failed: " + exception.getMessage();
        }

        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/login?error=" + encodedMessage);
    }
}
