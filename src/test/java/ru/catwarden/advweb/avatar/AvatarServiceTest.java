package ru.catwarden.advweb.avatar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.avatar.dto.AvatarDto;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.storage.FileUploader;
import ru.catwarden.advweb.storage.StoredFile;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private AvatarRepository avatarRepository;

    @Mock
    private AvatarMapper avatarMapper;

    @Mock
    private FileUploader fileUploader;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private AvatarService avatarService;

    @Test
    void uploadAvatarSavesAvatarAndReturnsDto() {
        StoredFile storedFile = new StoredFile("avatar.png", "/tmp/avatar.png");
        AvatarDto avatarDto = AvatarDto.builder().id(1L).url("/uploads/user_avatars/avatar.png").build();

        when(fileUploader.uploadFile(any(MultipartFile.class), any())).thenReturn(storedFile);
        when(avatarMapper.toDto(any(Avatar.class))).thenReturn(avatarDto);

        AvatarDto result;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");
            result = avatarService.uploadAvatar(multipartFile);
        }

        assertEquals(avatarDto, result);
        ArgumentCaptor<Avatar> avatarCaptor = ArgumentCaptor.forClass(Avatar.class);
        verify(avatarRepository).save(avatarCaptor.capture());
        Avatar savedAvatar = avatarCaptor.getValue();
        assertEquals("/tmp/avatar.png", savedAvatar.getPath());
        assertEquals("/uploads/user_avatars/avatar.png", savedAvatar.getUrl());
        assertEquals("kc-user", savedAvatar.getUploaderKeycloakId());
        assertFalse(savedAvatar.getLinkedToUser());
    }

    @Test
    void getUserAvatarReturnsMappedDto() {
        Avatar avatar = Avatar.builder().id(2L).url("/uploads/user_avatars/2.png").userId(44L).build();
        AvatarDto dto = AvatarDto.builder().id(2L).url("/uploads/user_avatars/2.png").build();
        when(avatarRepository.findByUserId(44L)).thenReturn(Optional.of(avatar));
        when(avatarMapper.toDto(avatar)).thenReturn(dto);

        AvatarDto result = avatarService.getUserAvatar(44L);

        assertEquals(dto, result);
    }

    @Test
    void getUserAvatarThrowsWhenUserHasNoAvatar() {
        when(avatarRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> avatarService.getUserAvatar(99L));
    }

    @Test
    void findUserAvatarUrlReturnsOptionalUrl() {
        Avatar avatar = Avatar.builder().id(2L).url("/uploads/user_avatars/2.png").userId(44L).build();
        when(avatarRepository.findByUserId(44L)).thenReturn(Optional.of(avatar));

        Optional<String> result = avatarService.findUserAvatarUrl(44L);

        assertTrue(result.isPresent());
        assertEquals("/uploads/user_avatars/2.png", result.get());
    }

    @Test
    void setAvatarToUserReplacesPreviousAvatarAndLinksNewOne() {
        Avatar newAvatar = Avatar.builder()
                .id(10L)
                .uploaderKeycloakId("owner-id")
                .linkedToUser(false)
                .userId(null)
                .build();
        Avatar oldAvatar = Avatar.builder()
                .id(9L)
                .uploaderKeycloakId("owner-id")
                .linkedToUser(true)
                .userId(5L)
                .build();
        when(avatarRepository.findById(10L)).thenReturn(Optional.of(newAvatar));
        when(avatarRepository.findByUserId(5L)).thenReturn(Optional.of(oldAvatar));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            avatarService.setAvatarToUser(10L, 5L);
        }

        assertFalse(oldAvatar.getLinkedToUser());
        assertEquals(null, oldAvatar.getUserId());
        assertTrue(newAvatar.getLinkedToUser());
        assertEquals(5L, newAvatar.getUserId());
        verify(avatarRepository).save(oldAvatar);
        verify(avatarRepository).save(newAvatar);
    }

    @Test
    void setAvatarToUserThrowsWhenAvatarLinkedToAnotherUser() {
        Avatar avatar = Avatar.builder()
                .id(10L)
                .uploaderKeycloakId("owner-id")
                .linkedToUser(true)
                .userId(7L)
                .build();
        when(avatarRepository.findById(10L)).thenReturn(Optional.of(avatar));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> avatarService.setAvatarToUser(10L, 5L));
            assertEquals("Avatar is linked to another user", exception.getMessage());
            assertEquals(
                    Map.of("Avatar id:", 10L, "Requested user id:", 5L, "Avatar is already linked:", true),
                    exception.getDetails()
            );
        }

        verify(avatarRepository, never()).save(any(Avatar.class));
    }

    @Test
    void setAvatarToUserThrowsWhenAvatarUploadedByAnotherUser() {
        Avatar avatar = Avatar.builder()
                .id(10L)
                .uploaderKeycloakId("another-user")
                .linkedToUser(false)
                .userId(null)
                .build();
        when(avatarRepository.findById(10L)).thenReturn(Optional.of(avatar));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> avatarService.setAvatarToUser(10L, 5L));
            assertEquals("Avatar was uploaded by another user", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Avatar id:", 10L,
                            "Avatar uploader keycloak id:", "another-user",
                            "Current user keycloak id:", "owner-id",
                            "Requested user id:", 5L
                    ),
                    exception.getDetails()
            );
        }

        verify(avatarRepository, never()).save(any(Avatar.class));
    }

    @Test
    void unlinkUserAvatarClearsLinkAndUserId() {
        Avatar avatar = Avatar.builder()
                .id(10L)
                .linkedToUser(true)
                .userId(5L)
                .build();
        when(avatarRepository.findByUserId(5L)).thenReturn(Optional.of(avatar));

        avatarService.unlinkUserAvatar(5L);

        assertFalse(avatar.getLinkedToUser());
        assertEquals(null, avatar.getUserId());
        verify(avatarRepository).save(avatar);
    }

    @Test
    void unlinkUserAvatarThrowsWhenAvatarMissing() {
        when(avatarRepository.findByUserId(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> avatarService.unlinkUserAvatar(5L));
    }
}

