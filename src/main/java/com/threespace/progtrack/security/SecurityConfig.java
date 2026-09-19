package com.threespace.progtrack.security;

import com.threespace.progtrack.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private CustomAuthenticationSuccessHandler successHandler;
    @Autowired private CustomAuthenticationFailureHandler failureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**", "/api/chat/**"))

            .authorizeHttpRequests(auth -> auth

                // ── Public ──────────────────────────────────────────────────
                .requestMatchers(
                    "/login", "/login-error", "/register",
                    "/css/**", "/js/**", "/images/**",
                    "/assets/**", "/vendor/**",
                    "/favicon.ico", "/manifest.json", "/sw.js",
                    "/error"
                ).permitAll()

                // ── ADMIN only ───────────────────────────────────────────────
                .requestMatchers(
                    "/admin/**", "/users/create", "/users/edit/**",
                    "/users/delete/**", "/system/**"
                ).hasRole("ADMIN")

                // ── CEO: company-wide ────────────────────────────────────────
                .requestMatchers(
                    "/analytics/**",
                    "/reports/company/**",
                    "/dashboard/executive/**"
                ).hasAnyRole("CEO", "ADMIN")

//                // ── Director ────────────────────────────────────────────────
//                .requestMatchers(
//                    "/reports/division/**",
//                    "/dashboard/director/**"
//                ).hasAnyRole("DIRECTOR", "CEO", "ADMIN")

                // ── Lead ──────────────────────────────────────────────────
                .requestMatchers(
                    "/lead/**",
                    "/reports/team/**",
                    "/leaves/approve/**",
                    "/leaves/reject/**",
                    "/leaves/manage/**",
                    "/tasks/team/**",
                    "/team/**"
                ).hasAnyRole("LEAD", "CEO", "ADMIN")

                // ── Reports (controller enforces role further) ────────────────
                .requestMatchers("/reports/**").hasAnyRole("LEAD", "CEO", "ADMIN")

                // ── Leaves (all authenticated — own leaves) ───────────────────
                .requestMatchers("/leaves/**").authenticated()

                // ── Tasks ────────────────────────────────────────────────────
                .requestMatchers("/tasks/**").authenticated()

                // ── Work log ────────────────────────────────────────────────
                .requestMatchers("/worklog/**").authenticated()

                // ── Chat + WebSocket ─────────────────────────────────────────
                .requestMatchers("/chat/**", "/ws/**", "/topic/**", "/app/**").authenticated()

                // ── Notifications ────────────────────────────────────────────
                .requestMatchers("/notifications/**").authenticated()

                // ── Profile ──────────────────────────────────────────────────
                .requestMatchers("/profile/**").authenticated()

                // ── APIs ─────────────────────────────────────────────────────
                .requestMatchers("/api/**").authenticated()

                // ── Dashboards ───────────────────────────────────────────────
                .requestMatchers("/dashboard/**", "/", "/home").authenticated()

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
            )

            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .permitAll()
            )

            .sessionManagement(session -> session
                .maximumSessions(3)
                .expiredUrl("/login?expired")
            )

            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )

            .authenticationProvider(authenticationProvider());

        return http.build();
    }
    
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}