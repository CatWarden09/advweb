package ru.catwarden.advweb.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.user.dto.UserResponse;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    private final UserMapper userMapper;

    public UserResponse getUser(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new EntityNotFoundException(User.class, id));
    }

    @Transactional
    public User syncUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    // Try to find by email if keycloakId is not found (for existing users)
                    String email = jwt.getClaimAsString("email");
                    Optional<User> existingUser = userRepository.findByEmail(email);

                    if (existingUser.isPresent()) {
                        User user = existingUser.get();
                        user.setKeycloakId(keycloakId);
                        return userRepository.save(user);
                    }

                    // Create new user if not found
                    User newUser = User.builder()
                            .keycloakId(keycloakId)
                            .email(email)
                            .firstName(jwt.getClaimAsString("given_name"))
                            .lastName(jwt.getClaimAsString("family_name"))
                            .phone(jwt.getClaimAsString("phone_number") != null ? jwt.getClaimAsString("phone_number") : "not_provided_" + keycloakId)
                            .rating(0.0)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    public User getByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, keycloakId));
    }

    public User getCurrentUser() {
        String keycloakId = ru.catwarden.advweb.security.SecurityUtils.getCurrentUserKeycloakId();
        if (keycloakId == null) return null;
        return userRepository.findByKeycloakId(keycloakId).orElse(null);
    }

    public boolean isCurrentUserOrAdmin(Long userId) {
        String currentKeycloakId = ru.catwarden.advweb.security.SecurityUtils.getCurrentUserKeycloakId();
        if (currentKeycloakId == null) return false;

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        return userRepository.findById(userId)
                .map(u -> currentKeycloakId.equals(u.getKeycloakId()))
                .orElse(false);
    }
}
