package com.ecobridge.config;

import com.ecobridge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// One-time deployment bootstrap: only an already-existing account can be promoted.
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private final UserRepository users;
    private final String email;

    public AdminBootstrap(UserRepository users, @Value("${ADMIN_BOOTSTRAP_EMAIL:}") String email) {
        this.users = users;
        this.email = email.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank()) return;
        users.findByEmailIgnoreCase(email).ifPresentOrElse(user -> {
            if (!"ACTIVE".equals(user.getAccountStatus())) {
                log.warn("Admin bootstrap skipped: matching account is not active");
                return;
            }
            if (!"ROLE_ADMIN".equals(user.getRole())) {
                user.setRole("ROLE_ADMIN");
                users.save(user);
            }
            log.info("Admin bootstrap confirmed for an existing account");
        }, () -> log.warn("Admin bootstrap skipped: matching account does not exist yet"));
    }
}
