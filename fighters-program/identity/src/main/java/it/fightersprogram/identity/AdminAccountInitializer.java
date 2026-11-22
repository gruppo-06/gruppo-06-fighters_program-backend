package it.fightersprogram.identity;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.identity.domain.AppUser;
import it.fightersprogram.identity.domain.Role;
import it.fightersprogram.identity.domain.UserRepository;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String nickname;

    public AdminAccountInitializer(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${identity.admin.email:}") String email,
            @Value("${identity.admin.password:}") String password,
            @Value("${identity.admin.nickname:admin}") String nickname) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim();
        this.password = password == null ? "" : password;
        this.nickname = (nickname == null || nickname.isBlank()) ? "admin" : nickname.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isEmpty() || password.isEmpty()) {
            log.info("Admin account seeding skipped: identity.admin.email or identity.admin.password not configured");
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "identity.admin.password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        Optional<AppUser> existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.ADMIN) {
                log.warn("User with email '{}' already exists with role {}; skipping admin seed without modifying it",
                        email, existing.get().getRole());
            } else {
                log.info("Admin account '{}' already exists; skipping creation", email);
            }
            return;
        }

        AppUser admin = AppUser.admin(nickname, email, passwordEncoder.encode(password));
        users.save(admin);
        log.info("Seeded initial admin account '{}' (nickname '{}')", email, nickname);
    }
}