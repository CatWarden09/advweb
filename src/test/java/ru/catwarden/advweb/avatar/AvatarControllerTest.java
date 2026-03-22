package ru.catwarden.advweb.avatar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.avatar.dto.AvatarDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarControllerTest {

    @Mock
    private AvatarService avatarService;

    @Mock
    private MultipartFile multipartFile;

    private AvatarController avatarController;

    @BeforeEach
    void setUp() {
        avatarController = new AvatarController(avatarService);
    }

    @Test
    void uploadAvatarDelegatesToService() {
        AvatarDto dto = AvatarDto.builder()
                .id(1L)
                .url("/uploads/user_avatars/new.png")
                .build();
        when(avatarService.uploadAvatar(multipartFile)).thenReturn(dto);

        AvatarDto result = avatarController.uploadAvatar(multipartFile);

        assertEquals(dto, result);
    }
}

