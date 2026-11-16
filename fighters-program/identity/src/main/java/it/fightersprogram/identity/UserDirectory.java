package it.fightersprogram.identity;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.identity.domain.AppUser;
import it.fightersprogram.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDirectory {

    private final UserRepository userRepository;

    public Optional<Long> findIdByNickname(String nickname) {
        return userRepository.findByNickname(nickname).map(AppUser::getId);
    }

    public String nicknameOf(long userId) {
        return userRepository.findById(userId)
                .map(AppUser::getNickname)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    public UserProfile profileOf(long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserProfile(user.getId(), user.getNickname(), user.getEmail(), user.getRole().name()))
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
