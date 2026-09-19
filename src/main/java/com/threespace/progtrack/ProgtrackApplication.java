package com.threespace.progtrack;

import com.threespace.progtrack.entity.User;
import com.threespace.progtrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProgtrackApplication {

    private static final Logger log = LoggerFactory.getLogger(ProgtrackApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ProgtrackApplication.class, args);
        log.info("🚀 ProgTrack started — http://localhost:8080");
    }

    /**
     * Seed default admin + demo users on first startup.
     * Skips if users already exist.
     */
    @Bean
    public CommandLineRunner dataInitializer(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("ℹ️  Database already seeded — skipping initialization.");
                return;
            }

            log.info("🌱 Seeding default users...");

            // ── ADMIN ──────────────────────────────────────────────────────────
            User admin = new User();
            admin.setEmail("admin@3space.com");
            admin.setPassword(encoder.encode("admin123"));
            admin.setFullName("System Administrator");
            admin.setDesignation("System Admin");
            admin.setDepartment("Admin");
            admin.setProjectGroup("Admin");
            admin.setRole(User.Role.ADMIN);
            admin.setIsActive(true);
            admin.setIsApproved(true);
            userRepository.save(admin);

            // ── CEO ────────────────────────────────────────────────────────────
            User ceo = new User();
            ceo.setEmail("ceo@3space.com");
            ceo.setPassword(encoder.encode("ceo123"));
            ceo.setFullName("Yashraj Patel");
            ceo.setDesignation("Chief Executive Officer");
            ceo.setDepartment("Executive");
            ceo.setProjectGroup("Executive");
            ceo.setRole(User.Role.CEO);
            ceo.setIsActive(true);
            ceo.setIsApproved(true);
            userRepository.save(ceo);



            // ── LEAD ────────────────────────────────────────────────────────
            User lead = new User();
            lead.setEmail("lead@3space.com");
            lead.setPassword(encoder.encode("lead123"));
            lead.setFullName("Aishwarya Raj");
            lead.setDesignation("Engineering Lead");
            lead.setDepartment("Engineering");
            lead.setProjectGroup("Backend Team");
            lead.setRole(User.Role.LEAD);
            lead.setIsActive(true);
            lead.setIsApproved(true);
            userRepository.save(lead);

            // ── INTERN ─────────────────────────────────────────────────────────
            User intern = new User();
            intern.setEmail("intern@3space.com");
            intern.setPassword(encoder.encode("intern123"));
            intern.setFullName("Sarthak Gandhi");
            intern.setDesignation("Software Engineering Intern");
            intern.setDepartment("Engineering");
            intern.setProjectGroup("Backend Team");
            intern.setRole(User.Role.INTERN);
            intern.setIsActive(true);
            intern.setIsApproved(true);
            userRepository.save(intern);

            log.info("✅ Default users created:");
            log.info("   admin@3space.com    / admin123   [ADMIN]");
            log.info("   ceo@3space.com      / ceo123     [CEO]");
            log.info("   lead@3space.com  / lead123     [LEAD]");
            log.info("   intern@3space.com   / intern123  [INTERN]");
        };
    }
}
