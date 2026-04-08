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
import ru.catwarden.advweb.ad.dto.AddressDto;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.ad.dto.AdvertisementUpdateRequest;
import ru.catwarden.advweb.adcategory.AdvertisementCategory;
import ru.catwarden.advweb.adcategory.CategoryRepository;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.enums.Status;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.InvalidRelationException;
import ru.catwarden.advweb.exception.InvalidStateException;
import ru.catwarden.advweb.exception.LimitExceededException;
import ru.catwarden.advweb.exception.OperationNotAllowedException;
import ru.catwarden.advweb.image.ImageService;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserRepository;
import ru.catwarden.advweb.user.UserService;

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
    @Mock
    private UserService userService;

    @InjectMocks
    private AdvertisementService advertisementService;

    @Test
    void getAdvertisementReturnsResponse() {
        User author = User.builder().id(5L).keycloakId("author-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(10L)
                .author(author)
                .status(Status.APPROVED)
                .build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(10L).build();

        when(advertisementRepository.findById(10L)).thenReturn(Optional.of(advertisement));
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getImageUrlsByAdvertisementId(10L)).thenReturn(List.of("image1.jpg", "image2.jpg"));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("some-user-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            AdvertisementResponse actual = advertisementService.getAdvertisement(10L);

            assertEquals(List.of("image1.jpg", "image2.jpg"), actual.getImageUrls());
        }
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

        when(advertisementRepository.findAllByStatus(Status.APPROVED, pageable)).thenReturn(page);
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

        when(advertisementRepository.findAllByStatus(Status.PENDING, pageable)).thenReturn(page);
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

        when(advertisementRepository.findAllByStatus(Status.REJECTED, pageable)).thenReturn(page);
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

        when(advertisementRepository.findAllByAuthorIdAndStatus(55L, Status.APPROVED, pageable))
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
        assertEquals(Status.PENDING, savedAdvertisement.getStatus());

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

            LimitExceededException exception = assertThrows(LimitExceededException.class,
                    () -> advertisementService.createAdvertisement(request));
            assertEquals("Limit for advertisement pictures is exceeded", exception.getMessage());
            assertEquals(
                    Map.of("User id:", 5L, "Number of pictures passed:", 11),
                    exception.getDetails()
            );
        }

        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void updateAdvertisementThrowsWhenCurrentUserHasNoAccess() {
        User author = User.builder().id(5L).keycloakId("owner-id").build();
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
        when(userRepository.findById(5L)).thenReturn(Optional.of(author));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> advertisementService.updateAdvertisement(7L, request));
            assertEquals("You can only view your own advertisements", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Requested user id:", 5L,
                            "Requested user keycloak id:", "owner-id"
                    ),
                    exception.getDetails()
            );
        }

        verify(imageService, never()).syncImagesInAdvertisement(any(), any());
    }

    @Test
    void updateAdvertisementReturnsWithoutChanges() {
        User author = User.builder().id(5L).keycloakId("owner-id").build();
        Address oldAddress = Address.builder().city("Moscow").street("A").house("1").build();
        Advertisement advertisement = Advertisement.builder()
                .id(7L)
                .author(author)
                .name("Same name")
                .description("Same description")
                .price(100.0)
                .address(oldAddress)
                .status(Status.APPROVED)
                .build();
        AdvertisementUpdateRequest request = validUpdateRequest("Same name", "Same description", 100.0, List.of(1L, 2L));

        when(advertisementRepository.findById(7L)).thenReturn(Optional.of(advertisement));
        when(userRepository.findById(5L)).thenReturn(Optional.of(author));
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
        assertEquals(Status.APPROVED, advertisement.getStatus());
    }

    @Test
    void updateAdvertisementSetsPendingWhenFieldsChanged() {
        User author = User.builder().id(5L).keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(7L)
                .author(author)
                .name("Old name")
                .description("Old description")
                .price(100.0)
                .address(Address.builder().city("Moscow").street("A").house("1").build())
                .status(Status.APPROVED)
                .build();
        AdvertisementUpdateRequest request = validUpdateRequest("New name", "New description", 150.0, List.of(1L, 2L));
        Address newAddress = Address.builder().city("Kazan").street("B").house("2").build();

        when(advertisementRepository.findById(7L)).thenReturn(Optional.of(advertisement));
        when(userRepository.findById(5L)).thenReturn(Optional.of(author));
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
        assertEquals(Status.PENDING, advertisement.getStatus());
    }

    @Test
    void updateAdvertisementThrowsWhenImageLimitExceeded() {
        User author = User.builder().id(6L).keycloakId("owner-id").build();
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
        when(userRepository.findById(6L)).thenReturn(Optional.of(author));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            LimitExceededException exception = assertThrows(LimitExceededException.class,
                    () -> advertisementService.updateAdvertisement(7L, request));
            assertEquals("Limit for advertisement pictures is exceeded", exception.getMessage());
            assertEquals(
                    Map.of("User id:", 6L, "Advertisement id:", 7L, "Number of pictures passed:", 11),
                    exception.getDetails()
            );
        }

        verify(imageService, never()).syncImagesInAdvertisement(any(), any());
    }

    @Test
    void updateAdvertisementThrowsWhenAdIsFinished() {
        User author = User.builder().id(6L).keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(7L)
                .author(author)
                .status(Status.FINISHED)
                .build();
        AdvertisementUpdateRequest request = validUpdateRequest("Name", "Desc", 100.0, List.of(1L));

        when(advertisementRepository.findById(7L)).thenReturn(Optional.of(advertisement));
        when(userRepository.findById(6L)).thenReturn(Optional.of(author));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            InvalidStateException exception = assertThrows(InvalidStateException.class,
                    () -> advertisementService.updateAdvertisement(7L, request));
            assertEquals("Cannot update a finished advertisement", exception.getMessage());
        }

        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void approveAdvertisementChangesStatusWhenPending() {
        Advertisement advertisement = Advertisement.builder()
                .id(50L)
                .status(Status.PENDING)
                .build();

        when(advertisementRepository.findById(50L)).thenReturn(Optional.of(advertisement));

        advertisementService.approveAdvertisement(50L);

        assertEquals(Status.APPROVED, advertisement.getStatus());
        verify(advertisementRepository).save(advertisement);
    }

    @Test
    void approveAdvertisementThrowsWhenStatusIsNotPending() {
        Advertisement advertisement = Advertisement.builder()
                .id(50L)
                .status(Status.REJECTED)
                .build();

        when(advertisementRepository.findById(50L)).thenReturn(Optional.of(advertisement));

        InvalidStateException exception = assertThrows(InvalidStateException.class,
                () -> advertisementService.approveAdvertisement(50L));
        assertEquals("Cannot change status of a non-pending advertisement", exception.getMessage());
        assertEquals(Map.of("Advertisement id:", 50L, "Current status:", Status.REJECTED), exception.getDetails());
        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void rejectAdvertisementChangesStatusAndReason() {
        Advertisement advertisement = Advertisement.builder()
                .id(51L)
                .status(Status.PENDING)
                .build();

        when(advertisementRepository.findById(51L)).thenReturn(Optional.of(advertisement));

        advertisementService.rejectAdvertisement(51L, "Incorrect description");

        assertEquals(Status.REJECTED, advertisement.getStatus());
        assertEquals("Incorrect description", advertisement.getModerationRejectionReason());
        verify(advertisementRepository).save(advertisement);
    }

    @Test
    void rejectAdvertisementThrowsWhenStatusIsNotPending() {
        Advertisement advertisement = Advertisement.builder()
                .id(51L)
                .status(Status.APPROVED)
                .build();

        when(advertisementRepository.findById(51L)).thenReturn(Optional.of(advertisement));

        InvalidStateException exception = assertThrows(InvalidStateException.class, () ->
                advertisementService.rejectAdvertisement(51L, "Reason"));
        assertEquals("Cannot change status of a non-pending advertisement", exception.getMessage());
        assertEquals(Map.of("Advertisement id:", 51L, "Current status:", Status.APPROVED), exception.getDetails());
        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void deleteAdvertisementThrowsWhenCurrentUserHasNoAccess() {
        User author = User.builder().id(5L).keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(12L)
                .author(author)
                .build();

        when(advertisementRepository.findById(12L)).thenReturn(Optional.of(advertisement));
        when(userRepository.findById(5L)).thenReturn(Optional.of(author));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> advertisementService.deleteAdvertisement(12L));
            assertEquals("You can only view your own advertisements", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Requested user id:", 5L,
                            "Requested user keycloak id:", "owner-id"
                    ),
                    exception.getDetails()
            );
        }

        verify(advertisementRepository, never()).deleteById(any());
    }

    @Test
    void deleteAdvertisementDeletesAdCommentsAndUnlinksImages() {
        User author = User.builder().id(5L).keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(12L)
                .author(author)
                .build();

        when(advertisementRepository.findById(12L)).thenReturn(Optional.of(advertisement));
        when(userRepository.findById(5L)).thenReturn(Optional.of(author));

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

            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class, () ->
                    advertisementService.getUserPendingAdvertisements(44L, PageRequest.of(0, 10)));
            assertEquals("You can only view your own advertisements", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Requested user id:", 44L,
                            "Requested user keycloak id:", "owner-id"
                    ),
                    exception.getDetails()
            );
        }

        verify(advertisementRepository, never())
                .findAllByAuthorIdAndStatus(any(), any(), any());
    }

    @Test
    void getUserPendingAdvertisementsForAdminReturnsMappedPage() {
        Advertisement advertisement = Advertisement.builder().id(1L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(1L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), PageRequest.of(0, 10), 1);

        when(advertisementRepository.findAllByAuthorIdAndStatus(
                eq(44L),
                eq(Status.PENDING),
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
        when(advertisementRepository.findAllByAuthorIdAndStatus(
                userId,
                Status.REJECTED,
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

    @Test
    void getUserFavoriteAdvertisementsReturnsApprovedFavoritesPage() {
        Long userId = 88L;
        User requestedUser = User.builder().id(userId).keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder().id(91L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(91L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), PageRequest.of(0, 10), 1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requestedUser));
        when(advertisementRepository.findFavoritesByUserIdAndStatus(
                userId,
                Status.APPROVED,
                PageRequest.of(0, 10)
        )).thenReturn(page);
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getPreviewImageUrlByAdvertisementId(91L)).thenReturn(List.of("favorite-preview.jpg"));

        Page<AdvertisementResponse> result;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");

            result = advertisementService.getUserFavoriteAdvertisements(userId, PageRequest.of(0, 10));
        }

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of("favorite-preview.jpg"), result.getContent().getFirst().getImageUrls());
    }

    @Test
    void addAdvertisementToFavoritesAddsApprovedAdvertisementForRequestedUser() {
        Long userId = 5L;
        User advertisementAuthor = User.builder()
                .id(10L)
                .keycloakId("author-kc")
                .build();
        Advertisement advertisement = Advertisement.builder()
                .id(100L)
                .status(Status.APPROVED)
                .author(advertisementAuthor)
                .build();
        User currentUser = User.builder()
                .id(userId)
                .keycloakId("kc-user")
                .favoriteAdvertisements(new java.util.ArrayList<>())
                .build();

        when(advertisementRepository.findById(100L)).thenReturn(Optional.of(advertisement));
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            advertisementService.addAdvertisementToFavorites(userId, 100L);
        }

        assertEquals(1, currentUser.getFavoriteAdvertisements().size());
        assertEquals(100L, currentUser.getFavoriteAdvertisements().getFirst().getId());
    }

    @Test
    void addAdvertisementToFavoritesThrowsForNotApprovedAdvertisement() {
        Long userId = 5L;
        Advertisement advertisement = Advertisement.builder()
                .id(100L)
                .status(Status.PENDING)
                .build();
        User requestedUser = User.builder().id(userId).keycloakId("kc-user").build();

        when(advertisementRepository.findById(100L)).thenReturn(Optional.of(advertisement));
        when(userRepository.findById(userId)).thenReturn(Optional.of(requestedUser));

        OperationNotAllowedException exception;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            exception = assertThrows(OperationNotAllowedException.class,
                    () -> advertisementService.addAdvertisementToFavorites(userId, 100L));
        }

        assertEquals("Only approved advertisements can be added to favorites", exception.getMessage());
        assertEquals(Map.of("Advertisement id:", 100L, "Current status:", Status.PENDING, "User id:", 5L), exception.getDetails());
    }

    @Test
    void removeAdvertisementFromFavoritesRemovesAdvertisementForRequestedUser() {
        Long userId = 5L;
        Advertisement favoriteAdvertisement = Advertisement.builder().id(100L).build();
        User currentUser = User.builder()
                .id(userId)
                .keycloakId("kc-user")
                .favoriteAdvertisements(new java.util.ArrayList<>(List.of(favoriteAdvertisement)))
                .build();
        User requestedUser = User.builder().id(userId).keycloakId("kc-user").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(requestedUser), Optional.of(currentUser));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            advertisementService.removeAdvertisementFromFavorites(userId, 100L);
        }

        assertTrue(currentUser.getFavoriteAdvertisements().isEmpty());
    }

    @Test
    void addAdvertisementToFavoritesThrowsForAnotherUser() {
        Long userId = 44L;
        User requestedUser = User.builder().id(userId).keycloakId("owner-id").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(requestedUser));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");

            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> advertisementService.addAdvertisementToFavorites(userId, 100L));
            assertEquals("You can only view your own advertisements", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Requested user id:", 44L,
                            "Requested user keycloak id:", "owner-id"
                    ),
                    exception.getDetails()
            );
        }

        verify(advertisementRepository, never()).findById(any());
    }

    @Test
    void finishAdvertisementChangesStatusWhenApproved() {
        User author = User.builder().id(5L).keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(60L)
                .status(Status.APPROVED)
                .author(author)
                .price(6000D)
                .build();

        when(advertisementRepository.findById(60L)).thenReturn(Optional.of(advertisement));
        when(userRepository.findById(5L)).thenReturn(Optional.of(author));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            advertisementService.finishAdvertisement(60L);
        }

        assertEquals(Status.FINISHED, advertisement.getStatus());
    }

    @Test
    void finishAdvertisementThrowsWhenNotApproved() {
        User author = User.builder().id(5L).keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(60L)
                .status(Status.PENDING)
                .author(author)
                .build();

        when(advertisementRepository.findById(60L)).thenReturn(Optional.of(advertisement));
        when(userRepository.findById(5L)).thenReturn(Optional.of(author));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            InvalidStateException exception = assertThrows(InvalidStateException.class,
                    () -> advertisementService.finishAdvertisement(60L));
            assertEquals("Cannot change status of a non-approved advertisement", exception.getMessage());
            assertEquals(Map.of("Advertisement id:", 60L, "Current status:", Status.PENDING), exception.getDetails());
        }

        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void finishAdvertisementThrowsWhenNotOwner() {
        User author = User.builder().id(5L).keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder()
                .id(60L)
                .status(Status.APPROVED)
                .author(author)
                .build();

        when(advertisementRepository.findById(60L)).thenReturn(Optional.of(advertisement));
        when(userRepository.findById(5L)).thenReturn(Optional.of(author));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> advertisementService.finishAdvertisement(60L));
            assertEquals("You can only view your own advertisements", exception.getMessage());
        }

        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void getUserFinishedAdvertisementsReturnsMappedPage() {
        Long userId = 77L;
        User requestedUser = User.builder().id(userId).keycloakId("owner-id").build();
        Advertisement advertisement = Advertisement.builder().id(71L).build();
        AdvertisementResponse response = AdvertisementResponse.builder().id(71L).build();
        Page<Advertisement> page = new PageImpl<>(List.of(advertisement), PageRequest.of(0, 10), 1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requestedUser));
        when(advertisementRepository.findAllByAuthorIdAndStatus(
                userId,
                Status.FINISHED,
                PageRequest.of(0, 10)
        )).thenReturn(page);
        when(advertisementMapper.toResponse(advertisement)).thenReturn(response);
        when(imageService.getPreviewImageUrlByAdvertisementId(71L)).thenReturn(List.of("finished.jpg"));

        Page<AdvertisementResponse> result;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");

            result = advertisementService.getUserFinishedAdvertisements(userId, PageRequest.of(0, 10));
        }

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of("finished.jpg"), result.getContent().getFirst().getImageUrls());
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
