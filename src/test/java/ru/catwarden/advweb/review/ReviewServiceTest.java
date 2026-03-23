package ru.catwarden.advweb.review;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.InvalidRelationException;
import ru.catwarden.advweb.exception.InvalidStateException;
import ru.catwarden.advweb.review.dto.ReviewRequest;
import ru.catwarden.advweb.review.dto.ReviewResponse;
import ru.catwarden.advweb.review.dto.ReviewUpdateRequest;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserRepository;
import ru.catwarden.advweb.user.UserResponseAssembler;
import ru.catwarden.advweb.user.UserService;
import ru.catwarden.advweb.user.dto.ShortUserInfoResponse;

import java.util.List;
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
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private UserResponseAssembler userResponseAssembler;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void getAllApprovedReviewsReturnsMappedPageWithAuthorInfo() {
        User author = User.builder().id(1L).build();
        Review review = Review.builder().id(7L).author(author).build();
        ReviewResponse response = ReviewResponse.builder().id(7L).build();
        ShortUserInfoResponse authorInfo = ShortUserInfoResponse.builder().id(1L).firstName("Ivan").build();

        when(reviewRepository.findByModerationStatus(AdModerationStatus.APPROVED, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));
        when(reviewMapper.toResponse(review)).thenReturn(response);
        when(userResponseAssembler.toShortUserInfoResponse(author)).thenReturn(authorInfo);

        Page<ReviewResponse> result = reviewService.getAllApprovedReviews(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(authorInfo, result.getContent().getFirst().getAuthorInfo());
    }

    @Test
    void getAllPendingReviewsReturnsMappedPageWithAuthorInfo() {
        User author = User.builder().id(2L).build();
        Review review = Review.builder().id(8L).author(author).build();
        ReviewResponse response = ReviewResponse.builder().id(8L).build();
        ShortUserInfoResponse authorInfo = ShortUserInfoResponse.builder().id(2L).firstName("Petr").build();

        when(reviewRepository.findByModerationStatus(AdModerationStatus.PENDING, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));
        when(reviewMapper.toResponse(review)).thenReturn(response);
        when(userResponseAssembler.toShortUserInfoResponse(author)).thenReturn(authorInfo);

        Page<ReviewResponse> result = reviewService.getAllPendingReviews(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(authorInfo, result.getContent().getFirst().getAuthorInfo());
    }

    @Test
    void getAllRejectedReviewsReturnsMappedPageWithAuthorInfo() {
        User author = User.builder().id(3L).build();
        Review review = Review.builder().id(9L).author(author).build();
        ReviewResponse response = ReviewResponse.builder().id(9L).build();
        ShortUserInfoResponse authorInfo = ShortUserInfoResponse.builder().id(3L).firstName("Alex").build();

        when(reviewRepository.findByModerationStatus(AdModerationStatus.REJECTED, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));
        when(reviewMapper.toResponse(review)).thenReturn(response);
        when(userResponseAssembler.toShortUserInfoResponse(author)).thenReturn(authorInfo);

        Page<ReviewResponse> result = reviewService.getAllRejectedReviews(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(authorInfo, result.getContent().getFirst().getAuthorInfo());
    }

    @Test
    void getApprovedReviewsAboutUserReturnsMappedPageWithAuthorInfo() {
        User author = User.builder().id(4L).build();
        Review review = Review.builder().id(10L).author(author).build();
        ReviewResponse response = ReviewResponse.builder().id(10L).build();
        ShortUserInfoResponse authorInfo = ShortUserInfoResponse.builder().id(4L).firstName("Oleg").build();

        when(reviewRepository.findByRecipientIdAndModerationStatus(55L, AdModerationStatus.APPROVED, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));
        when(reviewMapper.toResponse(review)).thenReturn(response);
        when(userResponseAssembler.toShortUserInfoResponse(author)).thenReturn(authorInfo);

        Page<ReviewResponse> result = reviewService.getApprovedReviewsAboutUser(55L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(authorInfo, result.getContent().getFirst().getAuthorInfo());
    }

    @Test
    void getUserReviewsThrowsWhenCurrentUserIsNotOwnerAndNotAdmin() {
        User requestedUser = User.builder().id(8L).keycloakId("owner-id").build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(requestedUser));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class, () ->
                    reviewService.getUserReviews(8L, PageRequest.of(0, 10), AdModerationStatus.PENDING));
            assertEquals("You can only view your own reviews", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Requested user id:", 8L,
                            "Requested user keycloak id:", "owner-id",
                            "Current user keycloak id:", "another-user"
                    ),
                    exception.getDetails()
            );
        }
    }

    @Test
    void getUserReviewsReturnsForOwner() {
        User requestedUser = User.builder().id(8L).keycloakId("owner-id").build();
        User author = User.builder().id(8L).keycloakId("owner-id").build();
        Review review = Review.builder().id(20L).author(author).build();
        ReviewResponse response = ReviewResponse.builder().id(20L).build();
        ShortUserInfoResponse authorInfo = ShortUserInfoResponse.builder().id(8L).firstName("Ivan").build();

        when(userRepository.findById(8L)).thenReturn(Optional.of(requestedUser));
        when(reviewRepository.findByAuthorIdAndModerationStatus(8L, AdModerationStatus.PENDING, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));
        when(reviewMapper.toResponse(review)).thenReturn(response);
        when(userResponseAssembler.toShortUserInfoResponse(author)).thenReturn(authorInfo);

        Page<ReviewResponse> result;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            result = reviewService.getUserReviews(8L, PageRequest.of(0, 10), AdModerationStatus.PENDING);
        }

        assertEquals(1, result.getTotalElements());
        assertEquals(authorInfo, result.getContent().getFirst().getAuthorInfo());
    }

    @Test
    void getUserReviewsReturnsForAdminWithoutUserLookup() {
        User author = User.builder().id(99L).build();
        Review review = Review.builder().id(21L).author(author).build();
        ReviewResponse response = ReviewResponse.builder().id(21L).build();
        ShortUserInfoResponse authorInfo = ShortUserInfoResponse.builder().id(99L).firstName("Admin").build();

        when(reviewRepository.findByAuthorIdAndModerationStatus(8L, AdModerationStatus.APPROVED, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));
        when(reviewMapper.toResponse(review)).thenReturn(response);
        when(userResponseAssembler.toShortUserInfoResponse(author)).thenReturn(authorInfo);

        Page<ReviewResponse> result;
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("admin-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(true);
            result = reviewService.getUserReviews(8L, PageRequest.of(0, 10), AdModerationStatus.APPROVED);
        }

        assertEquals(1, result.getTotalElements());
        verify(userRepository, never()).findById(any(Long.class));
    }

    @Test
    void getUserReviewsThrowsWhenRequestedUserNotFound() {
        when(userRepository.findById(8L)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            assertThrows(EntityNotFoundException.class, () ->
                    reviewService.getUserReviews(8L, PageRequest.of(0, 10), AdModerationStatus.PENDING));
        }
    }

    @Test
    void createReviewSavesPendingReview() {
        User currentUser = User.builder().id(1L).keycloakId("author-id").build();
        User recipient = User.builder().id(2L).keycloakId("recipient-id").build();
        ReviewRequest request = ReviewRequest.builder()
                .recipientId(2L)
                .text("This review text is long enough and meaningful for creating a pending review.")
                .rating(5)
                .build();
        Review review = Review.builder().text(request.getText()).rating(5).build();

        when(userRepository.findByKeycloakId("author-id")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(reviewMapper.toEntity(request)).thenReturn(review);

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("author-id");
            reviewService.createReview(request);
        }

        assertEquals(currentUser, review.getAuthor());
        assertEquals(recipient, review.getRecipient());
        assertEquals(AdModerationStatus.PENDING, review.getModerationStatus());
        verify(reviewRepository).save(review);
    }

    @Test
    void createReviewThrowsWhenCurrentUserNotFound() {
        ReviewRequest request = ReviewRequest.builder()
                .recipientId(2L)
                .text("This review text is long enough and meaningful for current user not found check.")
                .rating(5)
                .build();
        when(userRepository.findByKeycloakId("author-id")).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("author-id");
            assertThrows(EntityNotFoundException.class, () -> reviewService.createReview(request));
        }
    }

    @Test
    void createReviewThrowsWhenRecipientNotFound() {
        User currentUser = User.builder().id(1L).keycloakId("author-id").build();
        ReviewRequest request = ReviewRequest.builder()
                .recipientId(2L)
                .text("This review text is long enough and meaningful for recipient not found check.")
                .rating(5)
                .build();
        when(userRepository.findByKeycloakId("author-id")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("author-id");
            assertThrows(EntityNotFoundException.class, () -> reviewService.createReview(request));
        }
    }

    @Test
    void createReviewThrowsWhenUserReviewsHimself() {
        User currentUser = User.builder().id(1L).keycloakId("author-id").build();
        ReviewRequest request = ReviewRequest.builder()
                .recipientId(1L)
                .text("This review text is long enough and meaningful for self-review validation test.")
                .rating(5)
                .build();

        when(userRepository.findByKeycloakId("author-id")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("author-id");
            InvalidRelationException exception = assertThrows(InvalidRelationException.class,
                    () -> reviewService.createReview(request));
            assertEquals("Users cannot create reviews for themselves", exception.getMessage());
            assertEquals(Map.of("Current user id:", 1L, "Recipient user id:", 1L), exception.getDetails());
        }
    }

    @Test
    void updateReviewThrowsWhenCurrentUserHasNoAccess() {
        User author = User.builder().keycloakId("owner-id").build();
        Review review = Review.builder().id(4L).author(author).moderationStatus(AdModerationStatus.PENDING).build();
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .text("This updated text is long enough and meaningful for update authorization check.")
                .rating(4)
                .build();
        when(reviewRepository.findById(4L)).thenReturn(Optional.of(review));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> reviewService.updateReview(4L, request));
            assertEquals("You are not allowed to update this review", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Review id:", 4L,
                            "Review author keycloak id:", "owner-id",
                            "Current user keycloak id:", "another-user"
                    ),
                    exception.getDetails()
            );
        }
    }

    @Test
    void updateReviewThrowsWhenReviewNotFound() {
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .text("This updated text is long enough and meaningful for missing review update check.")
                .rating(4)
                .build();
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> reviewService.updateReview(404L, request));
    }

    @Test
    void updateReviewRecalculatesRatingWhenReviewWasApproved() {
        User author = User.builder().keycloakId("owner-id").build();
        User recipient = User.builder().id(9L).build();
        Review review = Review.builder()
                .id(4L)
                .author(author)
                .recipient(recipient)
                .moderationStatus(AdModerationStatus.APPROVED)
                .build();
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .text("This updated text is long enough and meaningful for approved review update test.")
                .rating(3)
                .build();
        when(reviewRepository.findById(4L)).thenReturn(Optional.of(review));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            reviewService.updateReview(4L, request);
        }

        assertEquals(request.getText(), review.getText());
        assertEquals(3, review.getRating());
        assertEquals(AdModerationStatus.PENDING, review.getModerationStatus());
        verify(reviewRepository).save(review);
        verify(userService).recalculateUserRating(9L);
    }

    @Test
    void updateReviewDoesNotRecalculateWhenReviewWasNotApproved() {
        User author = User.builder().keycloakId("owner-id").build();
        User recipient = User.builder().id(9L).build();
        Review review = Review.builder()
                .id(4L)
                .author(author)
                .recipient(recipient)
                .moderationStatus(AdModerationStatus.PENDING)
                .build();
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .text("This updated text is long enough and meaningful for pending review update test.")
                .rating(3)
                .build();
        when(reviewRepository.findById(4L)).thenReturn(Optional.of(review));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            reviewService.updateReview(4L, request);
        }

        verify(reviewRepository).save(review);
        verify(userService, never()).recalculateUserRating(any(Long.class));
    }

    @Test
    void updateReviewAllowsAdminEvenIfNotAuthor() {
        User author = User.builder().keycloakId("owner-id").build();
        User recipient = User.builder().id(9L).build();
        Review review = Review.builder()
                .id(4L)
                .author(author)
                .recipient(recipient)
                .moderationStatus(AdModerationStatus.PENDING)
                .build();
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .text("This updated text is long enough and meaningful for admin update path coverage.")
                .rating(4)
                .build();
        when(reviewRepository.findById(4L)).thenReturn(Optional.of(review));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("admin-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(true);
            reviewService.updateReview(4L, request);
        }

        verify(reviewRepository).save(review);
    }

    @Test
    void approveReviewChangesStatusAndRecalculatesRating() {
        User recipient = User.builder().id(11L).build();
        Review review = Review.builder().id(3L).moderationStatus(AdModerationStatus.PENDING).recipient(recipient).build();
        when(reviewRepository.findById(3L)).thenReturn(Optional.of(review));

        reviewService.approveReview(3L);

        assertEquals(AdModerationStatus.APPROVED, review.getModerationStatus());
        verify(reviewRepository).save(review);
        verify(userService).recalculateUserRating(11L);
    }

    @Test
    void approveReviewThrowsWhenReviewNotFound() {
        when(reviewRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> reviewService.approveReview(3L));
    }

    @Test
    void approveReviewThrowsWhenStatusIsNotPending() {
        Review review = Review.builder().id(3L).moderationStatus(AdModerationStatus.REJECTED).build();
        when(reviewRepository.findById(3L)).thenReturn(Optional.of(review));

        InvalidStateException exception = assertThrows(InvalidStateException.class, () -> reviewService.approveReview(3L));
        assertEquals("Cannot change status of a non-pending review", exception.getMessage());
        assertEquals(Map.of("Review id:", 3L, "Current status:", AdModerationStatus.REJECTED), exception.getDetails());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void rejectReviewChangesStatusAndReason() {
        Review review = Review.builder().id(3L).moderationStatus(AdModerationStatus.PENDING).build();
        when(reviewRepository.findById(3L)).thenReturn(Optional.of(review));

        reviewService.rejectReview(3L, "Bad language");

        assertEquals(AdModerationStatus.REJECTED, review.getModerationStatus());
        assertEquals("Bad language", review.getModerationRejectionReason());
        verify(reviewRepository).save(review);
    }

    @Test
    void rejectReviewThrowsWhenReviewNotFound() {
        when(reviewRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> reviewService.rejectReview(3L, "Bad language"));
    }

    @Test
    void rejectReviewThrowsWhenStatusIsNotPending() {
        Review review = Review.builder().id(3L).moderationStatus(AdModerationStatus.APPROVED).build();
        when(reviewRepository.findById(3L)).thenReturn(Optional.of(review));

        InvalidStateException exception = assertThrows(InvalidStateException.class,
                () -> reviewService.rejectReview(3L, "Bad language"));
        assertEquals("Cannot change status of a non-pending review", exception.getMessage());
        assertEquals(Map.of("Review id:", 3L, "Current status:", AdModerationStatus.APPROVED), exception.getDetails());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void deleteReviewThrowsWhenReviewNotExists() {
        when(reviewRepository.existsById(5L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> reviewService.deleteReview(5L));
    }

    @Test
    void deleteReviewThrowsWhenReviewMissingAfterExistsCheck() {
        when(reviewRepository.existsById(5L)).thenReturn(true);
        when(reviewRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> reviewService.deleteReview(5L));
    }

    @Test
    void deleteReviewThrowsWhenCurrentUserHasNoAccess() {
        User author = User.builder().keycloakId("owner-id").build();
        Review review = Review.builder()
                .id(5L)
                .author(author)
                .moderationStatus(AdModerationStatus.PENDING)
                .build();
        when(reviewRepository.existsById(5L)).thenReturn(true);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> reviewService.deleteReview(5L));
            assertEquals("You are not allowed to delete this review", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Review id:", 5L,
                            "Review author keycloak id:", "owner-id",
                            "Current user keycloak id:", "another-user"
                    ),
                    exception.getDetails()
            );
        }

        verify(reviewRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteReviewDoesNotRecalculateForNotApprovedReview() {
        User author = User.builder().keycloakId("owner-id").build();
        User recipient = User.builder().id(20L).build();
        Review review = Review.builder()
                .id(5L)
                .author(author)
                .recipient(recipient)
                .moderationStatus(AdModerationStatus.PENDING)
                .build();
        when(reviewRepository.existsById(5L)).thenReturn(true);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            reviewService.deleteReview(5L);
        }

        verify(reviewRepository).deleteById(5L);
        verify(userService, never()).recalculateUserRating(any(Long.class));
    }

    @Test
    void deleteReviewAllowsAdminEvenIfNotAuthor() {
        User author = User.builder().keycloakId("owner-id").build();
        User recipient = User.builder().id(20L).build();
        Review review = Review.builder()
                .id(5L)
                .author(author)
                .recipient(recipient)
                .moderationStatus(AdModerationStatus.PENDING)
                .build();
        when(reviewRepository.existsById(5L)).thenReturn(true);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("admin-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(true);
            reviewService.deleteReview(5L);
        }

        verify(reviewRepository).deleteById(5L);
    }

    @Test
    void deleteReviewRecalculatesRatingForApprovedReview() {
        User author = User.builder().keycloakId("owner-id").build();
        User recipient = User.builder().id(20L).build();
        Review review = Review.builder()
                .id(5L)
                .author(author)
                .recipient(recipient)
                .moderationStatus(AdModerationStatus.APPROVED)
                .build();
        when(reviewRepository.existsById(5L)).thenReturn(true);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("owner-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            reviewService.deleteReview(5L);
        }

        verify(reviewRepository).deleteById(5L);
        verify(userService).recalculateUserRating(20L);
    }
}
