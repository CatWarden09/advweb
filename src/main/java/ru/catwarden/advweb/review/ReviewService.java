package ru.catwarden.advweb.review;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.InvalidRelationException;
import ru.catwarden.advweb.exception.InvalidStateException;
import ru.catwarden.advweb.review.dto.ReviewRequest;
import ru.catwarden.advweb.review.dto.ReviewResponse;
import ru.catwarden.advweb.review.dto.ReviewUpdateRequest;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserMapper;
import ru.catwarden.advweb.user.UserRepository;
import ru.catwarden.advweb.user.UserService;

// DONE users cant leave reviews for themselves

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;

    public Page<ReviewResponse> getAllApprovedReviews(Pageable pageable) {
        return reviewRepository.findByModerationStatus(AdModerationStatus.APPROVED, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public Page<ReviewResponse> getAllPendingReviews(Pageable pageable) {
        return reviewRepository.findByModerationStatus(AdModerationStatus.PENDING, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public Page<ReviewResponse> getAllRejectedReviews(Pageable pageable) {
        return reviewRepository.findByModerationStatus(AdModerationStatus.REJECTED, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public Page<ReviewResponse> getApprovedReviewsAboutUser(Long userId, Pageable pageable) {
        return reviewRepository.findByRecipientIdAndModerationStatus(userId, AdModerationStatus.APPROVED, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public Page<ReviewResponse> getUserReviews(Long userId, Pageable pageable, AdModerationStatus status) {
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        validateCurrentUserOrAdmin(currentKeycloakId, userId);

        return reviewRepository.findByAuthorIdAndModerationStatus(userId, status, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public void createReview(ReviewRequest reviewRequest) {
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        User currentUser = userRepository.findByKeycloakId(currentKeycloakId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, currentKeycloakId));

        User recipient = userRepository.findById(reviewRequest.getRecipientId())
                .orElseThrow(() -> new EntityNotFoundException(User.class, reviewRequest.getRecipientId()));

        if (currentUser.equals(recipient)){
            throw new InvalidRelationException("Users cannot create reviews for themselves");
        }

        Review review = reviewMapper.toEntity(reviewRequest);

        review.setAuthor(currentUser);
        review.setRecipient(recipient);
        review.setModerationStatus(AdModerationStatus.PENDING);

        reviewRepository.save(review);
    }

    public void updateReview(Long id, ReviewUpdateRequest reviewUpdateRequest) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Review.class, id));
        boolean wasApproved = review.getModerationStatus() == AdModerationStatus.APPROVED;

        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();

        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && !review.getAuthor().getKeycloakId().equals(currentKeycloakId)) {
            throw new AccessDeniedException("You are not allowed to update this review");
        }

        review.setText(reviewUpdateRequest.getText());
        review.setRating(reviewUpdateRequest.getRating());
        review.setModerationStatus(AdModerationStatus.PENDING);

        reviewRepository.save(review);

        if (wasApproved) {
            userService.recalculateUserRating(review.getRecipient().getId());
        }
    }

    // DONE add status checking (cannot approve not pending)
    public void approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Review.class, id));

        if(review.getModerationStatus() != AdModerationStatus.PENDING){
            throw new InvalidStateException("Cannot change status of a non-pending review");
        }

        review.setModerationStatus(AdModerationStatus.APPROVED);

        reviewRepository.save(review);
        userService.recalculateUserRating(review.getRecipient().getId());
    }

    public void rejectReview(Long id, String moderationRejectionReason) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Review.class, id));

        if(review.getModerationStatus() != AdModerationStatus.PENDING){
            throw new InvalidStateException("Cannot change status of a non-pending review");
        }

        review.setModerationStatus(AdModerationStatus.REJECTED);
        review.setModerationRejectionReason(moderationRejectionReason);

        reviewRepository.save(review);
    }

    public void deleteReview(Long id) {
        if(!reviewRepository.existsById(id)){
            throw new EntityNotFoundException(Review.class, id);
        }

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Review.class, id));
        boolean wasApproved = review.getModerationStatus() == AdModerationStatus.APPROVED;


        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();

        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && !review.getAuthor().getKeycloakId().equals(currentKeycloakId)) {
            throw new AccessDeniedException("You are not allowed to delete this review");
        }

        reviewRepository.deleteById(id);

        if (wasApproved) {
            userService.recalculateUserRating(review.getRecipient().getId());
        }
    }

    private void validateCurrentUserOrAdmin(String currentKeycloakId, Long userId) {
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (isAdmin) {
            return;
        }

        User requestedUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, userId));

        if (!currentKeycloakId.equals(requestedUser.getKeycloakId())) {
            throw new AccessDeniedException("You can only view your own reviews");
        }
    }

    private ReviewResponse mapWithShortUserInfo(Review review){
        ReviewResponse response = reviewMapper.toResponse(review);
        response.setAuthorInfo(userMapper.toShortUserInfoResponse(review.getAuthor()));

        return response;
    }
}



