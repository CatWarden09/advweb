package ru.catwarden.advweb.ad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import ru.catwarden.advweb.ad.dto.AddressDto;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.ad.dto.AdvertisementUpdateRequest;
import ru.catwarden.advweb.adcategory.AdvertisementCategory;
import ru.catwarden.advweb.adcategory.CategoryRepository;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.exception.InvalidRelationException;
import ru.catwarden.advweb.exception.InvalidStateException;
import ru.catwarden.advweb.exception.LimitExceededException;
import ru.catwarden.advweb.image.ImageService;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvertisementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AdvertisementRepository advertisementRepository;
    @Mock
    private AdvertisementMapper advertisementMapper;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private ImageService imageService;
    @Mock
    private CommentService commentService;
    @Mock
    private ViewCountService viewCountService;

    @InjectMocks
    private AdvertisementService advertisementService;

    @Test
    void getAdvertisementReturnsResponseAndIncrementsViews() {
        Advertisement advertisement = Advertisement.builder().id(10L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(10L).build();

        when(advertisementRepository.findById(10L)).thenReturn(Optional.of(advertisement));
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getPreviewImageUrlByAdvertisementId(10L)).thenReturn(List.of("preview.jpg"));

        AdvertisementResponse actual = advertisementService.getAdvertisement(10L);

        assertEquals(List.of("preview.jpg"), actual.getImageUrls());
        verify(viewCountService).increment(10L);
    }

    @Test
    void getAdvertisementsByFilterReturnsMappedPageWithImages() {
        Pageable pageable = PageRequest.of(0, 10);
        Advertisement advertisement = Advertisement.builder().id(20L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(20L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), pageable, 1);

        when(advertisementRepository.findAll(any(com.querydsl.core.types.Predicate.class), eq(pageable))).thenReturn(page);
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getImageUrlsByAdvertisementId(20L)).thenReturn(List.of("img1.jpg", "img2.jpg"));

        Page<AdvertisementResponse> result = advertisementService.getAdvertisementsByFilter(
                pageable,
                ru.catwarden.advweb.ad.dto.AdvertisementSearchFilter.builder()
                        .name("bike")
                        .priceMin(100.0)
                        .priceMax(1000.0)
                        .build()
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of("img1.jpg", "img2.jpg"), result.getContent().getFirst().getImageUrls());
    }

    @Test
    void getAdvertisementsByFilterCoversOtherFilterBranches() {
        Pageable pageable = PageRequest.of(0, 10);
        Advertisement advertisement = Advertisement.builder().id(21L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(21L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), pageable, 1);

        when(advertisementRepository.findAll(any(com.querydsl.core.types.Predicate.class), eq(pageable))).thenReturn(page);
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getImageUrlsByAdvertisementId(21L)).thenReturn(List.of("img3.jpg"));

        Page<AdvertisementResponse> result = advertisementService.getAdvertisementsByFilter(
                pageable,
                ru.catwarden.advweb.ad.dto.AdvertisementSearchFilter.builder()
                        .description("good condition")
                        .categoryId(10L)
                        .subcategoryId(11L)
                        .build()
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of("img3.jpg"), result.getContent().getFirst().getImageUrls());
    }

    @Test
    void getAllApprovedAdvertisementsReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Advertisement advertisement = Advertisement.builder().id(30L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(30L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), pageable, 1);

        when(advertisementRepository.findAllByAdModerationStatus(AdModerationStatus.APPROVED, pageable)).thenReturn(page);
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getPreviewImageUrlByAdvertisementId(30L)).thenReturn(List.of("preview.jpg"));

        Page<AdvertisementResponse> result = advertisementService.getAllApprovedAdvertisements(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of("preview.jpg"), result.getContent().getFirst().getImageUrls());
    }

    @Test
    void getAllPendingAdvertisementsReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Advertisement advertisement = Advertisement.builder().id(31L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(31L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), pageable, 1);

        when(advertisementRepository.findAllByAdModerationStatus(AdModerationStatus.PENDING, pageable)).thenReturn(page);
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getPreviewImageUrlByAdvertisementId(31L)).thenReturn(List.of("pending-preview.jpg"));

        Page<AdvertisementResponse> result = advertisementService.getAllPendingAdvertisements(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of("pending-preview.jpg"), result.getContent().getFirst().getImageUrls());
    }

    @Test
    void getAllRejectedAdvertisementsReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Advertisement advertisement = Advertisement.builder().id(32L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(32L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), pageable, 1);

        when(advertisementRepository.findAllByAdModerationStatus(AdModerationStatus.REJECTED, pageable)).thenReturn(page);
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getPreviewImageUrlByAdvertisementId(32L)).thenReturn(List.of("rejected-preview.jpg"));

        Page<AdvertisementResponse> result = advertisementService.getAllRejectedAdvertisements(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of("rejected-preview.jpg"), result.getContent().getFirst().getImageUrls());
    }

    @Test
    void getUserApprovedAdvertisementsReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Advertisement advertisement = Advertisement.builder().id(33L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(33L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), pageable, 1);

        when(advertisementRepository.findAllByAuthorIdAndAdModerationStatus(55L, AdModerationStatus.APPROVED, pageable))
                .thenReturn(page);
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getPreviewImageUrlByAdvertisementId(33L)).thenReturn(List.of("approved-user-preview.jpg"));

        Page<AdvertisementResponse> result = advertisementService.getUserApprovedAdvertisements(55L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of("approved-user-preview.jpg"), result.getContent().getFirst().getImageUrls());
    }

    @Test
    void createAdvertisementCreatesPendingAdvertisementAndLinksImages() {
        AdvertisementRequest request = validCreateRequest(List.of(1L, 2L, 3L));

        User author = User.builder().id(5L).keycloakId("kc-user").build();
        AdvertisementCategory category = AdvertisementCategory.builder().id(100L).build();
        AdvertisementCategory subcategory = AdvertisementCategory.builder().id(101L).parent(category).build();

        Advertisement mappedEntity = Advertisement.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .build();
        Address mappedAddress = Address.builder().city("Moscow").street("Lenina").house("1").build();

        when(userRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(author));
        when(categoryRepository.findById(100L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(101L)).thenReturn(Optional.of(subcategory));
        when(advertisementMapper.toEntity(request)).thenReturn(mappedEntity);
        when(addressMapper.toEntity(request.getAddress())).thenReturn(mappedAddress);
        when(advertisementRepository.save(any(Advertisement.class))).thenAnswer(invocation -> {
            Advertisement saved = invocation.getArgument(0);
            saved.setId(999L);
            return saved;
        });

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");

            Long createdId = advertisementService.createAdvertisement(request);

            assertEquals(999L, createdId);
        }

        ArgumentCaptor<Advertisement> advertisementCaptor = ArgumentCaptor.forClass(Advertisement.class);
        verify(advertisementRepository).save(advertisementCaptor.capture());
        Advertisement savedAdvertisement = advertisementCaptor.getValue();

        assertEquals(author, savedAdvertisement.getAuthor());
        assertEquals(category, savedAdvertisement.getCategory());
        assertEquals(subcategory, savedAdvertisement.getSubcategory());
        assertEquals(mappedAddress, savedAdvertisement.getAddress());
        assertEquals(AdModerationStatus.PENDING, savedAdvertisement.getAdModerationStatus());

        verify(imageService).setImagesToAdvertisement(request.getImageIds(), 999L);
    }

    @Test
    void createAdvertisementThrowsWhenSubcategoryIsNotChild() {
        AdvertisementRequest request = validCreateRequest(List.of(1L, 2L));

        User author = User.builder().id(5L).keycloakId("kc-user").build();
        AdvertisementCategory category = AdvertisementCategory.builder().id(100L).build();
        AdvertisementCategory anotherParent = AdvertisementCategory.builder().id(200L).build();
        AdvertisementCategory subcategory = AdvertisementCategory.builder().id(101L).parent(anotherParent).build();

        when(userRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(author));
        when(categoryRepository.findById(100L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(101L)).thenReturn(Optional.of(subcategory));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");

            InvalidRelationException exception = assertThrows(InvalidRelationException.class,
                    () -> advertisementService.createAdvertisement(request));
            assertEquals("Subcategory is not a child of the given category", exception.getMessage());
            assertEquals(Map.of("Subcategory id:", 101L, "Parent category id:", 100L), exception.getDetails());
        }

        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void createAdvertisementThrowsWhenSubcategoryEqualsCategory() {
        AdvertisementRequest request = validCreateRequest(List.of(1L, 2L));

        User author = User.builder().id(5L).keycloakId("kc-user").build();
        AdvertisementCategory category = AdvertisementCategory.builder().id(100L).build();
        category.setParent(category);

        when(userRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(author));
        when(categoryRepository.findById(100L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(101L)).thenReturn(Optional.of(category));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");

            InvalidRelationException exception = assertThrows(InvalidRelationException.class,
                    () -> advertisementService.createAdvertisement(request));
            assertEquals("Subcategory cannot be the same as the category", exception.getMessage());
            assertEquals(Map.of("Subcategory id:", 100L, "Parent category id:", 100L), exception.getDetails());
        }

        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void createAdvertisementThrowsWhenImageLimitExceeded() {
        AdvertisementRequest request = validCreateRequest(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L));

        User author = User.builder().id(5L).keycloakId("kc-user").build();
        AdvertisementCategory category = AdvertisementCategory.builder().id(100L).build();
        AdvertisementCategory subcategory = AdvertisementCategory.builder().id(101L).parent(category).build();

        when(userRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(author));
        when(categoryRepository.findById(100L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(101L)).thenReturn(Optional.of(subcategory));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");

            assertThrows(LimitExceededException.class, () -> advertisementService.createAdvertisement(request));
        }

        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void updateAdvertisementThrowsWhenCurrentUserHasNoAccess() {
        User author = User.builder().keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(7L)
                .author(author)
                .name("Old name")
                .description("Old description")
                .price(100.0)
                .address(Address.builder().city("Moscow").street("A").house("1").build())
                .build();
        AdvertisementUpdateRequest request = validUpdateRequest("New name", "New description", 120.0, List.of(1L));

        when(advertisementRepository.findById(7L)).thenReturn(Optional.of(advertisement));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            assertThrows(AccessDeniedException.class, () -> advertisementService.updateAdvertisement(7L, request));
        }

        verify(imageService, never()).syncImagesInAdvertisement(any(), any());
    }

    @Test
    void updateAdvertisementReturnsWithoutChanges() {
        User author = User.builder().keycloakId("owner-id").build();
        Address oldAddress = Address.builder().city("Moscow").street("A").house("1").build();
        Advertisement advertisement = Advertisement.builder()
                .id(7L)
                .author(author)
                .name("Same name")
                .description("Same description")
                .price(100.0)
                .address(oldAddress)
                .adModerationStatus(AdModerationStatus.APPROVED)
                .build();
        AdvertisementUpdateRequest request = validUpdateRequest("Same name", "Same description", 100.0, List.of(1L, 2L));

        when(advertisementRepository.findById(7L)).thenReturn(Optional.of(advertisement));
        when(addressMapper.toEntity(request.getAddress())).thenReturn(Address.builder().city("Moscow").street("A").house("1").build());
        when(imageService.syncImagesInAdvertisement(7L, request.getImageIds())).thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            advertisementService.updateAdvertisement(7L, request);
        }

        assertEquals("Same name", advertisement.getName());
        assertEquals("Same description", advertisement.getDescription());
        assertEquals(100.0, advertisement.getPrice());
        assertEquals(AdModerationStatus.APPROVED, advertisement.getAdModerationStatus());
    }

    @Test
    void updateAdvertisementSetsPendingWhenFieldsChanged() {
        User author = User.builder().keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(7L)
                .author(author)
                .name("Old name")
                .description("Old description")
                .price(100.0)
                .address(Address.builder().city("Moscow").street("A").house("1").build())
                .adModerationStatus(AdModerationStatus.APPROVED)
                .build();
        AdvertisementUpdateRequest request = validUpdateRequest("New name", "New description", 150.0, List.of(1L, 2L));
        Address newAddress = Address.builder().city("Kazan").street("B").house("2").build();

        when(advertisementRepository.findById(7L)).thenReturn(Optional.of(advertisement));
        when(addressMapper.toEntity(request.getAddress())).thenReturn(newAddress);
        when(imageService.syncImagesInAdvertisement(7L, request.getImageIds())).thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            advertisementService.updateAdvertisement(7L, request);
        }

        assertEquals("New name", advertisement.getName());
        assertEquals("New description", advertisement.getDescription());
        assertEquals(150.0, advertisement.getPrice());
        assertEquals(newAddress, advertisement.getAddress());
        assertEquals(AdModerationStatus.PENDING, advertisement.getAdModerationStatus());
    }

    @Test
    void updateAdvertisementThrowsWhenImageLimitExceeded() {
        User author = User.builder().keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(7L)
                .author(author)
                .name("Old name")
                .description("Old description")
                .price(100.0)
                .address(Address.builder().city("Moscow").street("A").house("1").build())
                .build();
        AdvertisementUpdateRequest request = validUpdateRequest(
                "Old name",
                "Old description",
                100.0,
                List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L)
        );

        when(advertisementRepository.findById(7L)).thenReturn(Optional.of(advertisement));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            assertThrows(LimitExceededException.class, () -> advertisementService.updateAdvertisement(7L, request));
        }

        verify(imageService, never()).syncImagesInAdvertisement(any(), any());
    }

    @Test
    void approveAdvertisementChangesStatusWhenPending() {
        Advertisement advertisement = Advertisement.builder()
                .id(50L)
                .adModerationStatus(AdModerationStatus.PENDING)
                .build();

        when(advertisementRepository.findById(50L)).thenReturn(Optional.of(advertisement));

        advertisementService.approveAdvertisement(50L);

        assertEquals(AdModerationStatus.APPROVED, advertisement.getAdModerationStatus());
        verify(advertisementRepository).save(advertisement);
    }

    @Test
    void approveAdvertisementThrowsWhenStatusIsNotPending() {
        Advertisement advertisement = Advertisement.builder()
                .id(50L)
                .adModerationStatus(AdModerationStatus.REJECTED)
                .build();

        when(advertisementRepository.findById(50L)).thenReturn(Optional.of(advertisement));

        assertThrows(InvalidStateException.class, () -> advertisementService.approveAdvertisement(50L));
        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void rejectAdvertisementChangesStatusAndReason() {
        Advertisement advertisement = Advertisement.builder()
                .id(51L)
                .adModerationStatus(AdModerationStatus.PENDING)
                .build();

        when(advertisementRepository.findById(51L)).thenReturn(Optional.of(advertisement));

        advertisementService.rejectAdvertisement(51L, "Incorrect description");

        assertEquals(AdModerationStatus.REJECTED, advertisement.getAdModerationStatus());
        assertEquals("Incorrect description", advertisement.getModerationRejectionReason());
        verify(advertisementRepository).save(advertisement);
    }

    @Test
    void rejectAdvertisementThrowsWhenStatusIsNotPending() {
        Advertisement advertisement = Advertisement.builder()
                .id(51L)
                .adModerationStatus(AdModerationStatus.APPROVED)
                .build();

        when(advertisementRepository.findById(51L)).thenReturn(Optional.of(advertisement));

        assertThrows(InvalidStateException.class, () ->
                advertisementService.rejectAdvertisement(51L, "Reason"));
        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void deleteAdvertisementThrowsWhenCurrentUserHasNoAccess() {
        User author = User.builder().keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(12L)
                .author(author)
                .build();

        when(advertisementRepository.findById(12L)).thenReturn(Optional.of(advertisement));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            assertThrows(AccessDeniedException.class, () -> advertisementService.deleteAdvertisement(12L));
        }

        verify(advertisementRepository, never()).deleteById(any());
    }

    @Test
    void deleteAdvertisementDeletesAdCommentsAndUnlinksImages() {
        User author = User.builder().keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(12L)
                .author(author)
                .build();

        when(advertisementRepository.findById(12L)).thenReturn(Optional.of(advertisement));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            advertisementService.deleteAdvertisement(12L);
        }

        verify(advertisementRepository).deleteById(12L);
        verify(commentService).deleteCommentsByAdId(12L);
        verify(imageService).unlinkAllImagesFromAdvertisement(12L);
    }

    @Test
    void getUserPendingAdvertisementsThrowsForAnotherUser() {
        User requestedUser = User.builder().id(44L).keycloakId("owner-id").build();
        when(userRepository.findById(44L)).thenReturn(Optional.of(requestedUser));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");

            assertThrows(AccessDeniedException.class, () ->
                    advertisementService.getUserPendingAdvertisements(44L, PageRequest.of(0, 10)));
        }

        verify(advertisementRepository, never())
                .findAllByAuthorIdAndAdModerationStatus(any(), any(), any());
    }

    @Test
    void getUserPendingAdvertisementsForAdminReturnsMappedPage() {
        Advertisement advertisement = Advertisement.builder().id(1L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(1L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), PageRequest.of(0, 10), 1);

        when(advertisementRepository.findAllByAuthorIdAndAdModerationStatus(
                eq(44L),
                eq(AdModerationStatus.PENDING),
                any(PageRequest.class)
        )).thenReturn(page);
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getPreviewImageUrlByAdvertisementId(1L)).thenReturn(List.of("url1"));

        Page<AdvertisementResponse> result;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(true);

            result = advertisementService.getUserPendingAdvertisements(44L, PageRequest.of(0, 10));
        }

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of("url1"), result.getContent().getFirst().getImageUrls());
        assertTrue(result.getContent().getFirst().getId().equals(1L));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getUserRejectedAdvertisementsForOwnerReturnsMappedPage() {
        Long userId = 77L;
        User requestedUser = User.builder().id(userId).keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder().id(71L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(71L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), PageRequest.of(0, 10), 1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requestedUser));
        when(advertisementRepository.findAllByAuthorIdAndAdModerationStatus(
                userId,
                AdModerationStatus.REJECTED,
                PageRequest.of(0, 10)
        )).thenReturn(page);
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getPreviewImageUrlByAdvertisementId(71L)).thenReturn(List.of("rejected.jpg"));

        Page<AdvertisementResponse> result;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");

            result = advertisementService.getUserRejectedAdvertisements(userId, PageRequest.of(0, 10));
        }

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of("rejected.jpg"), result.getContent().getFirst().getImageUrls());
    }

    private AdvertisementRequest validCreateRequest(List<Long> imageIds) {
        return AdvertisementRequest.builder()
                .name("Nice bike")
                .description("Very good bike, almost new")
                .price(450.0)
                .address(AddressDto.builder().city("Moscow").street("Lenina").house("1").build())
                .categoryId(100L)
                .subcategoryId(101L)
                .imageIds(imageIds)
                .build();
    }

    private AdvertisementUpdateRequest validUpdateRequest(
            String name,
            String description,
            Double price,
            List<Long> imageIds
    ) {
        return AdvertisementUpdateRequest.builder()
                .name(name)
                .description(description)
                .price(price)
                .address(AddressDto.builder().city("Moscow").street("A").house("1").build())
                .imageIds(imageIds)
                .build();
    }
}
