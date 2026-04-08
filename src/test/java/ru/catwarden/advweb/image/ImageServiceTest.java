package ru.catwarden.advweb.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.FileStorageException;
import ru.catwarden.advweb.image.dto.ImageDto;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.storage.FileUploader;
import ru.catwarden.advweb.storage.StoredFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;
    @Mock
    private ImageMapper imageMapper;
    @Mock
    private FileUploader fileUploader;
    @Mock
    private MultipartFile file1;
    @Mock
    private MultipartFile file2;

    @InjectMocks
    private ImageService imageService;

    @Test
    void uploadImageSavesAndReturnsDtos() {
        List<MultipartFile> files = List.of(file1, file2);
        List<StoredFile> storedFiles = List.of(
                new StoredFile("1.png", "/tmp/1.png"),
                new StoredFile("2.png", "/tmp/2.png")
        );
        List<Image> savedImages = List.of(
                Image.builder().id(1L).url("/uploads/adv_photos/1.png").build(),
                Image.builder().id(2L).url("/uploads/adv_photos/2.png").build()
        );

        when(fileUploader.uploadFiles(any(), any())).thenReturn(storedFiles);
        when(imageRepository.saveAll(any())).thenReturn(savedImages);
        when(imageMapper.toDto(savedImages.get(0))).thenReturn(ImageDto.builder().id(1L).url("/uploads/adv_photos/1.png").build());
        when(imageMapper.toDto(savedImages.get(1))).thenReturn(ImageDto.builder().id(2L).url("/uploads/adv_photos/2.png").build());

        List<ImageDto> result;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");
            result = imageService.uploadImage(files);
        }

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());

        ArgumentCaptor<List<Image>> imageCaptor = ArgumentCaptor.forClass(List.class);
        verify(imageRepository).saveAll(imageCaptor.capture());
        List<Image> uploaded = imageCaptor.getValue();
        assertEquals(2, uploaded.size());
        assertEquals("kc-user", uploaded.get(0).getUploaderKeycloakId());
        assertFalse(uploaded.get(0).getLinkedToAd());
        assertEquals("/uploads/adv_photos/1.png", uploaded.get(0).getUrl());
    }

    @Test
    void getPreviewImageUrlByAdvertisementIdReturnsSingleUrl() {
        Image image = Image.builder().id(1L).url("/uploads/adv_photos/1.png").build();
        when(imageRepository.findFirstByAdId(5L)).thenReturn(Optional.of(image));

        List<String> result = imageService.getPreviewImageUrlByAdvertisementId(5L);

        assertEquals(1, result.size());
        assertEquals("/uploads/adv_photos/1.png", result.getFirst());
    }

    @Test
    void getPreviewImageUrlByAdvertisementIdThrowsWhenMissing() {
        when(imageRepository.findFirstByAdId(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> imageService.getPreviewImageUrlByAdvertisementId(5L));
    }

    @Test
    void getImageUrlsByAdvertisementIdReturnsAllUrls() {
        when(imageRepository.findAllByAdId(5L)).thenReturn(List.of(
                Image.builder().id(1L).url("/uploads/adv_photos/1.png").build(),
                Image.builder().id(2L).url("/uploads/adv_photos/2.png").build()
        ));

        List<String> result = imageService.getImageUrlsByAdvertisementId(5L);

        assertEquals(List.of("/uploads/adv_photos/1.png", "/uploads/adv_photos/2.png"), result);
    }

    @Test
    void setImagesToAdvertisementLinksImagesWhenValid() {
        List<Long> requestIds = List.of(1L, 2L);
        Image image1 = Image.builder().id(1L).uploaderKeycloakId("owner").linkedToAd(false).adId(null).build();
        Image image2 = Image.builder().id(2L).uploaderKeycloakId("owner").linkedToAd(false).adId(null).build();
        when(imageRepository.findAllById(requestIds)).thenReturn(List.of(image1, image2));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            imageService.setImagesToAdvertisement(requestIds, 10L);
        }

        assertTrue(image1.getLinkedToAd());
        assertTrue(image2.getLinkedToAd());
        assertEquals(10L, image1.getAdId());
        assertEquals(10L, image2.getAdId());
        verify(imageRepository).saveAll(List.of(image1, image2));
    }

    @Test
    void setImagesToAdvertisementThrowsWhenSomeImageMissing() {
        List<Long> requestIds = List.of(1L, 2L);
        Image image1 = Image.builder().id(1L).uploaderKeycloakId("owner").linkedToAd(false).build();
        when(imageRepository.findAllById(requestIds)).thenReturn(List.of(image1));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            assertThrows(EntityNotFoundException.class, () -> imageService.setImagesToAdvertisement(requestIds, 10L));
        }
    }

    @Test
    void setImagesToAdvertisementThrowsWhenImageLinkedToAnotherAd() {
        List<Long> requestIds = List.of(1L);
        Image image = Image.builder()
                .id(1L)
                .uploaderKeycloakId("owner")
                .linkedToAd(true)
                .adId(999L)
                .build();
        when(imageRepository.findAllById(requestIds)).thenReturn(List.of(image));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> imageService.setImagesToAdvertisement(requestIds, 10L));
            assertEquals("One or more images are linked to another advertisement", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Requested advertisement id:", 10L,
                            "Conflicting image id:", 1L,
                            "Conflicting image advertisement id:", "999"
                    ),
                    exception.getDetails()
            );
        }
    }

    @Test
    void setImagesToAdvertisementThrowsWhenImageUploadedByAnotherUser() {
        List<Long> requestIds = List.of(1L);
        Image image = Image.builder()
                .id(1L)
                .uploaderKeycloakId("another-user")
                .linkedToAd(false)
                .adId(null)
                .build();
        when(imageRepository.findAllById(requestIds)).thenReturn(List.of(image));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> imageService.setImagesToAdvertisement(requestIds, 10L));
            assertEquals("One or more images were uploaded by another user", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Requested advertisement id:", 10L,
                            "Conflicting image id:", 1L,
                            "Conflicting image uploader keycloak id:", "another-user"
                    ),
                    exception.getDetails()
            );
        }
    }

    @Test
    void syncImagesInAdvertisementReturnsFalseWhenImageSetsEqual() {
        when(imageRepository.findAllByAdId(7L)).thenReturn(List.of(
                Image.builder().id(1L).build(),
                Image.builder().id(2L).build()
        ));

        boolean result = imageService.syncImagesInAdvertisement(7L, List.of(2L, 1L));

        assertFalse(result);
        verify(imageRepository, never()).unlinkImagesMissingFromAdvertisement(any(Long.class), any(List.class));
    }

    @Test
    void syncImagesInAdvertisementUnlinksRemovedAndLinksRequested() {
        when(imageRepository.findAllByAdId(7L)).thenReturn(List.of(
                Image.builder().id(1L).build(),
                Image.builder().id(2L).build(),
                Image.builder().id(3L).build()
        ));

        List<Long> requestIds = List.of(2L, 3L, 4L);
        Image image2 = Image.builder().id(2L).uploaderKeycloakId("owner").linkedToAd(true).adId(7L).build();
        Image image3 = Image.builder().id(3L).uploaderKeycloakId("owner").linkedToAd(true).adId(7L).build();
        Image image4 = Image.builder().id(4L).uploaderKeycloakId("owner").linkedToAd(false).adId(null).build();
        when(imageRepository.findAllById(requestIds)).thenReturn(List.of(image2, image3, image4));

        boolean result;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            result = imageService.syncImagesInAdvertisement(7L, requestIds);
        }

        assertTrue(result);
        verify(imageRepository).unlinkImagesMissingFromAdvertisement(7L, requestIds);
        verify(imageRepository).saveAll(List.of(image2, image3, image4));
        assertEquals(7L, image4.getAdId());
        assertTrue(image4.getLinkedToAd());
    }

    @Test
    void unlinkAllImagesFromAdvertisementClearsLinks() {
        Image image1 = Image.builder().id(1L).adId(7L).linkedToAd(true).build();
        Image image2 = Image.builder().id(2L).adId(7L).linkedToAd(true).build();
        when(imageRepository.findAllByAdId(7L)).thenReturn(List.of(image1, image2));

        imageService.unlinkAllImagesFromAdvertisement(7L);

        assertEquals(null, image1.getAdId());
        assertEquals(null, image2.getAdId());
        assertFalse(image1.getLinkedToAd());
        assertFalse(image2.getLinkedToAd());
        verify(imageRepository).saveAll(List.of(image1, image2));
    }

    @Test
    void deleteUnusedImagesDeletesFilesAndRecords() throws IOException {
        Image image1 = Image.builder().id(1L).path("/tmp/1.png").linkedToAd(false).build();
        Image image2 = Image.builder().id(2L).path("/tmp/2.png").linkedToAd(false).build();
        when(imageRepository.findAllByLinkedToAdFalse()).thenReturn(List.of(image1, image2));

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.deleteIfExists(Paths.get("/tmp/1.png"))).thenReturn(true);
            filesMock.when(() -> Files.deleteIfExists(Paths.get("/tmp/2.png"))).thenReturn(true);

            imageService.deleteUnusedImages();

            filesMock.verify(() -> Files.deleteIfExists(Paths.get("/tmp/1.png")));
            filesMock.verify(() -> Files.deleteIfExists(Paths.get("/tmp/2.png")));
        }

        verify(imageRepository).deleteAll(List.of(image1, image2));
    }

    @Test
    void deleteUnusedImagesThrowsWhenFileDeletionFails() throws IOException {
        Image image = Image.builder().id(1L).path("/tmp/1.png").linkedToAd(false).build();
        when(imageRepository.findAllByLinkedToAdFalse()).thenReturn(List.of(image));

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.deleteIfExists(Paths.get("/tmp/1.png")))
                    .thenThrow(new IOException("disk error"));

            assertThrows(FileStorageException.class, () -> imageService.deleteUnusedImages());
        }

        verify(imageRepository, never()).deleteAll(anyList());
    }
}

