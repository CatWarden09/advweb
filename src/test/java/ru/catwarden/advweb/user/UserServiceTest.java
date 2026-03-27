package ru.catwarden.advweb.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.catwarden.advweb.avatar.AvatarService;
import ru.catwarden.advweb.enums.Status;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.OperationNotAllowedException;
import ru.catwarden.advweb.review.ReviewRepository;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.user.dto.UserResponse;
import ru.catwarden.advweb.user.dto.UserUpdateRequest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private AvatarService avatarService;
    @Mock
    private UserResponseAssembler userResponseAssembler;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserReturnsAssemblerResult() {
        User user = User.builder().id(1L).build();
        UserResponse response = UserResponse.builder().id(1L).firstName("Ivan").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userResponseAssembler.toUserResponse(user)).thenReturn(response);

        UserResponse result = userService.getUser(1L);

        assertEquals(response, result);
    }

    @Test
    void updateUserThrowsWhenCurrentUserHasNoAccess() {
        User user = User.builder().id(1L).keycloakId("owner-id").build();
        UserUpdateRequest request = validUpdateRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> userService.updateUser(1L, request));
            assertEquals("You are not allowed to update this user", exception.getMessage());
            assertEquals(
                    Map.of(
                            "User id:", 1L,
                            "User keycloak id:", "owner-id",
                            "Actor id:", "another-user"
                    ),
                    exception.getDetails()
            );
        }
    }

    @Test
    void updateUserThrowsWhenEmailAlreadyUsed() {
        User user = User.builder()
                .id(1L)
                .keycloakId("owner-id")
                .firstName("OldFirstName")
                .lastName("OldLastName")
                .email("old@mail.com")
                .phone("+7000")
                .build();
        User otherUser = User.builder().id(2L).email("taken@mail.com").build();

        UserUpdateRequest request = validUpdateRequest();
        request.setEmail("taken@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("taken@mail.com")).thenReturn(Optional.of(otherUser));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                    () -> userService.updateUser(1L, request));
            assertEquals("Email is already in use", exception.getMessage());
            assertEquals(
                    Map.of("Actor id:", "owner-id", "Passed email:", "taken@mail.com", "User with existing email id", 2L),
                    exception.getDetails()
            );
        }
    }

    @Test
    void updateUserThrowsWhenPhoneAlreadyUsed() {
        User user = User.builder()
                .id(1L)
                .keycloakId("owner-id")
                .firstName("OldFirstName")
                .lastName("OldLastName")
                .email("old@mail.com")
                .phone("+7000")
                .build();
        User otherUser = User.builder().id(2L).phone("+7111").build();
        UserUpdateRequest request = validUpdateRequest();
        request.setPhone("+7111");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByPhone("+7111")).thenReturn(Optional.of(otherUser));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
                    () -> userService.updateUser(1L, request));
            assertEquals("Phone is already in use", exception.getMessage());
            assertEquals(
                    Map.of("Actor id:", "owner-id", "Passed phone:", "+7111", "User with existing phone id", 2L),
                    exception.getDetails()
            );
        }
    }

    @Test
    void updateUserUpdatesFieldsAndReturnsResponse() {
        User user = User.builder()
                .id(1L)
                .keycloakId("owner-id")
                .firstName("OldFirstName")
                .lastName("OldLastName")
                .email("old@mail.com")
                .phone("+7000")
                .build();
        UserUpdateRequest request = validUpdateRequest();
        UserResponse response = UserResponse.builder().id(1L).firstName("Ivan").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByPhone(request.getPhone())).thenReturn(Optional.empty());
        when(userResponseAssembler.toUserResponse(user)).thenReturn(response);

        UserResponse result;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            result = userService.updateUser(1L, request);
        }

        assertEquals("Ivan", user.getFirstName());
        assertEquals("Petrov", user.getLastName());
        assertEquals("+79990000000", user.getPhone());
        assertEquals("ivan@example.com", user.getEmail());
        assertEquals(response, result);
    }

    @Test
    void setUserAvatarUpdatesAvatarIdAndCallsAvatarService() {
        User user = User.builder().id(1L).keycloakId("owner-id").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            userService.setUserAvatar(1L, 55L);
        }

        assertEquals(55L, user.getAvatarId());
        verify(avatarService).setAvatarToUser(55L, 1L);
    }

    @Test
    void unlinkUserAvatarClearsAvatarIdAndCallsAvatarService() {
        User user = User.builder().id(1L).keycloakId("owner-id").avatarId(55L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            userService.unlinkUserAvatar(1L);
        }

        assertEquals(null, user.getAvatarId());
        verify(avatarService).unlinkUserAvatar(1L);
    }

    @Test
    void syncUserReturnsExistingWhenKeycloakIdAlreadyLinked() {
        User existing = User.builder().id(1L).keycloakId("kc-id").build();
        when(userRepository.findByKeycloakId("kc-id")).thenReturn(Optional.of(existing));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-id")
                .claim("email", "ivan@example.com")
                .claim("given_name", "Ivan")
                .claim("family_name", "Petrov")
                .build();

        User result = userService.syncUser(jwt);

        assertEquals(existing, result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void syncUserLinksExistingUserByEmail() {
        User existing = User.builder().id(1L).email("ivan@example.com").keycloakId(null).build();
        when(userRepository.findByKeycloakId("kc-id")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-id")
                .claim("email", "ivan@example.com")
                .claim("given_name", "Ivan")
                .claim("family_name", "Petrov")
                .build();

        User result = userService.syncUser(jwt);

        assertEquals("kc-id", existing.getKeycloakId());
        assertEquals(existing, result);
        verify(userRepository).save(existing);
    }

    @Test
    void syncUserCreatesNewUserWithFallbackPhoneWhenMissing() {
        when(userRepository.findByKeycloakId("kc-id")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-id")
                .claim("email", "ivan@example.com")
                .claim("given_name", "Ivan")
                .claim("family_name", "Petrov")
                .build();

        User result = userService.syncUser(jwt);

        assertEquals("kc-id", result.getKeycloakId());
        assertEquals("ivan@example.com", result.getEmail());
        assertEquals("Ivan", result.getFirstName());
        assertEquals("Petrov", result.getLastName());
        assertEquals("not_provided_kc-id", result.getPhone());
        assertEquals(0.0, result.getRating());
        assertEquals(0L, result.getRatingCount());
    }

    @Test
    void recalculateUserRatingThrowsWhenUserMissing() {
        when(userRepository.existsById(9L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> userService.recalculateUserRating(9L));
    }

    @Test
    void recalculateUserRatingUpdatesStatsFromAggregate() {
        when(userRepository.existsById(9L)).thenReturn(true);
        when(reviewRepository.aggregateRatingByRecipientAndStatus(9L, Status.APPROVED))
                .thenReturn(Optional.of(new Object[]{4.5, 2L}));

        userService.recalculateUserRating(9L);

        verify(userRepository).updateUserRatingStats(9L, 4.5, 2L);
    }

    private UserUpdateRequest validUpdateRequest() {
        return UserUpdateRequest.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .phone("+79990000000")
                .email("ivan@example.com")
                .build();
    }
}

