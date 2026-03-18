package ru.catwarden.advweb.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.OperationNotAllowedException;
import ru.catwarden.advweb.review.ReviewRepository;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.user.dto.UserResponse;
import ru.catwarden.advweb.user.dto.UserUpdateRequest;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    private final UserMapper userMapper;

    public UserResponse getUser(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new EntityNotFoundException(User.class, id));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest userUpdateRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(User.class, id));

        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        validateCurrentUserCanUpdate(user, currentKeycloakId);

        userRepository.findByEmail(userUpdateRequest.getEmail())
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new OperationNotAllowedException("Email is already in use");
                });

        userRepository.findByPhone(userUpdateRequest.getPhone())
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new OperationNotAllowedException("Phone is already in use");
                });

        user.setFirstName(userUpdateRequest.getFirstName());
        user.setLastName(userUpdateRequest.getLastName());
        user.setPhone(userUpdateRequest.getPhone());
        user.setEmail(userUpdateRequest.getEmail());

        return userMapper.toUserResponse(user);
    }

    // this method is used to sync the keycloak user with the local user (for example when registering a new user, we get the jwt token and create a new local user with the given keycloakId)
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
                            .ratingCount(0L)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public void recalculateUserRating(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException(User.class, userId);
        }

        Object[] aggregate = reviewRepository
                .aggregateRatingByRecipientAndStatus(userId, AdModerationStatus.APPROVED)
                .orElse(new Object[]{0.0, 0L});

        Double rating = aggregate[0] != null ? ((Number) aggregate[0]).doubleValue() : 0.0;
        Long ratingCount = aggregate[1] != null ? ((Number) aggregate[1]).longValue() : 0L;

        userRepository.updateUserRatingStats(userId, rating, ratingCount);
    }

    public boolean isCurrentUserOrAdmin(Long userId) {
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        if (currentKeycloakId == null) return false;

        if (SecurityUtils.isCurrentUserAdmin()) {
            return true;
        }

        return userRepository.findById(userId)
                .map(u -> currentKeycloakId.equals(u.getKeycloakId()))
                .orElse(false);
    }

    private void validateCurrentUserCanUpdate(User user, String currentKeycloakId) {
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && !Objects.equals(user.getKeycloakId(), currentKeycloakId)) {
            throw new AccessDeniedException("You are not allowed to update this user");
        }
    }
}
