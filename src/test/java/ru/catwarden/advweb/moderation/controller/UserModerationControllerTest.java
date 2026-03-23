package ru.catwarden.advweb.moderation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.catwarden.advweb.user.UserService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserModerationControllerTest {

    @Mock
    private UserService userService;

    private UserModerationController controller;

    @BeforeEach
    void setUp() {
        controller = new UserModerationController(userService);
    }

    @Test
    void deleteUserAvatarDelegatesToService() {
        controller.deleteUserAvatar(11L);

        verify(userService).unlinkUserAvatar(11L);
    }
}
