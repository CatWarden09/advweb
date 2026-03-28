package ru.catwarden.advweb.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.catwarden.advweb.ad.AdvertisementService;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.enums.Status;
import ru.catwarden.advweb.review.ReviewService;
import ru.catwarden.advweb.review.dto.ReviewResponse;
import ru.catwarden.advweb.user.dto.UserResponse;
import ru.catwarden.advweb.user.dto.UserUpdateRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private AdvertisementService advertisementService;
    @Mock
    private ReviewService reviewService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService, advertisementService, reviewService);
    }

    @Test
    void getUserReturnsServiceResult() {
        UserResponse response = UserResponse.builder().id(1L).firstName("Ivan").build();
        when(userService.getUser(1L)).thenReturn(response);

        UserResponse result = userController.getUser(1L);

        assertEquals(response, result);
    }

    @Test
    void updateUserDelegatesToService() {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .phone("+79990000000")
                .email("ivan@example.com")
                .build();
        UserResponse response = UserResponse.builder().id(1L).firstName("Ivan").build();
        when(userService.updateUser(1L, request)).thenReturn(response);

        UserResponse result = userController.updateUser(1L, request);

        assertEquals(response, result);
    }

    @Test
    void getUserApprovedAdvertisementsPassesPageable() {
        Page<AdvertisementResponse> page = new PageImpl<>(List.of(AdvertisementResponse.builder().id(1L).build()));
        when(advertisementService.getUserApprovedAdvertisements(any(Long.class), any(Pageable.class))).thenReturn(page);

        Page<AdvertisementResponse> result = userController.getUserApprovedAdvertisements(5L, 2, 20);

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(advertisementService).getUserApprovedAdvertisements(org.mockito.ArgumentMatchers.eq(5L), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    void getUserPendingAdvertisementsPassesPageable() {
        Page<AdvertisementResponse> page = new PageImpl<>(List.of());
        when(advertisementService.getUserPendingAdvertisements(any(Long.class), any(Pageable.class))).thenReturn(page);

        userController.getUserPendingAdvertisements(5L, 1, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(advertisementService).getUserPendingAdvertisements(org.mockito.ArgumentMatchers.eq(5L), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void getUserRejectedAdvertisementsPassesPageable() {
        Page<AdvertisementResponse> page = new PageImpl<>(List.of());
        when(advertisementService.getUserRejectedAdvertisements(any(Long.class), any(Pageable.class))).thenReturn(page);

        userController.getUserRejectedAdvertisements(5L, 3, 15);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(advertisementService).getUserRejectedAdvertisements(org.mockito.ArgumentMatchers.eq(5L), captor.capture());
        assertEquals(3, captor.getValue().getPageNumber());
        assertEquals(15, captor.getValue().getPageSize());
    }

    @Test
    void getUserFinishedAdvertisementsPassesPageable() {
        Page<AdvertisementResponse> page = new PageImpl<>(List.of());
        when(advertisementService.getUserFinishedAdvertisements(any(Long.class), any(Pageable.class))).thenReturn(page);

        userController.getUserFinishedAdvertisements(5L, 0, 10);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(advertisementService).getUserFinishedAdvertisements(org.mockito.ArgumentMatchers.eq(5L), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    void getUserFavoriteAdvertisementsPassesPageable() {
        Page<AdvertisementResponse> page = new PageImpl<>(List.of());
        when(advertisementService.getUserFavoriteAdvertisements(any(Long.class), any(Pageable.class))).thenReturn(page);

        userController.getUserFavoriteAdvertisements(5L, 4, 12);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(advertisementService).getUserFavoriteAdvertisements(org.mockito.ArgumentMatchers.eq(5L), captor.capture());
        assertEquals(4, captor.getValue().getPageNumber());
        assertEquals(12, captor.getValue().getPageSize());
    }

    @Test
    void addAdvertisementToFavoritesDelegatesToService() {
        userController.addAdvertisementToFavorites(5L, 99L);

        verify(advertisementService).addAdvertisementToFavorites(5L, 99L);
    }

    @Test
    void removeAdvertisementFromFavoritesDelegatesToService() {
        userController.removeAdvertisementFromFavorites(5L, 99L);

        verify(advertisementService).removeAdvertisementFromFavorites(5L, 99L);
    }

    @Test
    void getApprovedReviewsAboutUserPassesPageable() {
        Page<ReviewResponse> page = new PageImpl<>(List.of(ReviewResponse.builder().id(1L).build()));
        when(reviewService.getApprovedReviewsAboutUser(any(Long.class), any(Pageable.class))).thenReturn(page);

        Page<ReviewResponse> result = userController.getApprovedReviewsAboutUser(5L, 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(reviewService).getApprovedReviewsAboutUser(org.mockito.ArgumentMatchers.eq(5L), any(Pageable.class));
    }

    @Test
    void getUserRejectedReviewsPassesRejectedStatus() {
        when(reviewService.getUserReviews(any(Long.class), any(Pageable.class), any(Status.class)))
                .thenReturn(new PageImpl<>(List.of()));

        userController.getUserRejectedReviews(5L, 0, 10);

        verify(reviewService).getUserReviews(org.mockito.ArgumentMatchers.eq(5L), any(Pageable.class), org.mockito.ArgumentMatchers.eq(Status.REJECTED));
    }

    @Test
    void getUserPendingReviewsPassesPendingStatus() {
        when(reviewService.getUserReviews(any(Long.class), any(Pageable.class), any(Status.class)))
                .thenReturn(new PageImpl<>(List.of()));

        userController.getUserPendingReviews(5L, 0, 10);

        verify(reviewService).getUserReviews(org.mockito.ArgumentMatchers.eq(5L), any(Pageable.class), org.mockito.ArgumentMatchers.eq(Status.PENDING));
    }

    @Test
    void getUserApprovedReviewsPassesApprovedStatus() {
        when(reviewService.getUserReviews(any(Long.class), any(Pageable.class), any(Status.class)))
                .thenReturn(new PageImpl<>(List.of()));

        userController.getUserApprovedReviews(5L, 0, 10);

        verify(reviewService).getUserReviews(org.mockito.ArgumentMatchers.eq(5L), any(Pageable.class), org.mockito.ArgumentMatchers.eq(Status.APPROVED));
    }

    @Test
    void setUserAvatarDelegatesToService() {
        userController.setUserAvatar(5L, 99L);

        verify(userService).setUserAvatar(5L, 99L);
    }

    @Test
    void deleteUserAvatarDelegatesToService() {
        userController.deleteUserAvatar(5L);

        verify(userService).unlinkUserAvatar(5L);
    }
}

