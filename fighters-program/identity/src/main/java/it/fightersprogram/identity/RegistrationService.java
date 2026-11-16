package it.fightersprogram.identity;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.identity.domain.AppUser;
import it.fightersprogram.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    @Transactional
    public UserProfile register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new ConflictException("Nickname already taken: " + request.nickname());
        }
        AppUser user = saveOrConflict(new AppUser(request.nickname(), request.email(),
                passwordEncoder.encode(request.password())));
        events.publishEvent(new UserRegistered(user.getId()));
        return new UserProfile(user.getId(), user.getNickname(), user.getEmail(), user.getRole().name());
    }

    private AppUser saveOrConflict(AppUser user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Email or nickname already registered");
        }
    }
}
