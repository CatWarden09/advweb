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
import org.springframework.security.access.AccessDeniedException;
import ru.catwarden.advweb.enums.AdModerationStatus;
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
    void getUserReviewsThrowsWhenCurrentUserIsNotOwnerAndNotAdmin() {
        User requestedUser = User.builder().id(8L).keycloakId("owner-id").build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(requestedUser));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            assertThrows(AccessDeniedException.class, () ->
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
            assertThrows(InvalidRelationException.class, () -> reviewService.createReview(request));
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
            assertThrows(AccessDeniedException.class, () -> reviewService.updateReview(4L, request));
        }
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
    void approveReviewThrowsWhenStatusIsNotPending() {
        Review review = Review.builder().id(3L).moderationStatus(AdModerationStatus.REJECTED).build();
        when(reviewRepository.findById(3L)).thenReturn(Optional.of(review));

        assertThrows(InvalidStateException.class, () -> reviewService.approveReview(3L));
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
    void deleteReviewThrowsWhenReviewNotExists() {
        when(reviewRepository.existsById(5L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> reviewService.deleteReview(5L));
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

