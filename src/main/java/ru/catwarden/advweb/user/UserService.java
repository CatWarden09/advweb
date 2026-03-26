package ru.catwarden.advweb.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import ru.catwarden.advweb.avatar.AvatarService;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.OperationNotAllowedException;
import ru.catwarden.advweb.review.ReviewRepository;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.user.dto.UserResponse;
import ru.catwarden.advweb.user.dto.UserUpdateRequest;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static ru.catwarden.advweb.ad.QAdvertisement.advertisement;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final AvatarService avatarService;

    private final UserResponseAssembler userResponseAssembler;

    @Cacheable(value = "users", key = "#id")
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(User.class, id));

        return userResponseAssembler.toUserResponse(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponse updateUser(Long id, UserUpdateRequest userUpdateRequest) {
        boolean isFieldsChanged = false;

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(User.class, id));


        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        validateCurrentUserCanUpdate(user, currentKeycloakId);

        // protection from NLP
        if (!Objects.equals(user.getFirstName(), userUpdateRequest.getFirstName())
                || !Objects.equals(user.getLastName(), userUpdateRequest.getLastName())
                || !Objects.equals(user.getEmail(), userUpdateRequest.getEmail())
                || !Objects.equals(user.getPhone(), userUpdateRequest.getPhone())) {

            isFieldsChanged = true;
        }

        if (!isFieldsChanged){
            return userResponseAssembler.toUserResponse(user);
        }

        userRepository.findByEmail(userUpdateRequest.getEmail())
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new OperationNotAllowedException("Email is already in use",
                            Map.of("Current user id:", id, "Passed email:", userUpdateRequest.getEmail() ,"User with existing email id", existingUser.getId()));
                });

        userRepository.findByPhone(userUpdateRequest.getPhone())
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new OperationNotAllowedException("Phone is already in use",
                            Map.of("Current user id:", id, "Passed phone:", userUpdateRequest.getPhone() ,"User with existing phone id", existingUser.getId()));
                });

        user.setFirstName(userUpdateRequest.getFirstName());
        user.setLastName(userUpdateRequest.getLastName());
        user.setPhone(userUpdateRequest.getPhone());
        user.setEmail(userUpdateRequest.getEmail());

        log.info("AUDIT User updated: userId={}", id);

        return userResponseAssembler.toUserResponse(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void setUserAvatar(Long userId, Long avatarId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, userId));

        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        validateCurrentUserCanUpdate(user, currentKeycloakId);

        avatarService.setAvatarToUser(avatarId, userId);
        user.setAvatarId(avatarId);

        log.info("AUDIT User avatar set: userId={}, avatarId={}", userId, avatarId);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void unlinkUserAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, userId));

        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        validateCurrentUserCanUpdate(user, currentKeycloakId);

        avatarService.unlinkUserAvatar(userId);

        user.setAvatarId(null);

        log.info("AUDIT User avatar unlinked: userId={}", userId);
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
    @CacheEvict(value = "users", key = "#userId")
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

        log.info("AUDIT User rating recalculated: userId={}, rating={}, ratingCount={}", userId, rating, ratingCount);
    }

    private void validateCurrentUserCanUpdate(User user, String currentKeycloakId) {
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && !Objects.equals(user.getKeycloakId(), currentKeycloakId)) {
            throw new DetailedAccessDeniedException("You are not allowed to update this user",
                    Map.of(
                            "User id:", user.getId(),
                            "User keycloak id:", String.valueOf(user.getKeycloakId()),
                            "Current user keycloak id:", String.valueOf(currentKeycloakId)
                    ));
        }
    }
}
