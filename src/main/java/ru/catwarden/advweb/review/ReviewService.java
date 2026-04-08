package ru.catwarden.advweb.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.enums.Status;
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

import java.util.Map;

// DONE users cant leave reviews for themselves

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    private final ReviewMapper reviewMapper;
    private final UserResponseAssembler userResponseAssembler;

    @Cacheable(
            value = "reviews-approved-list",
            key = "'approved-p-' + #pageable.pageNumber + '-s-' + #pageable.pageSize + '-sort-' + #pageable.sort",
            condition = "#pageable.pageNumber == 0"
    )
    public Page<ReviewResponse> getAllApprovedReviews(Pageable pageable) {
        return reviewRepository.findByStatus(Status.APPROVED, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public Page<ReviewResponse> getAllPendingReviews(Pageable pageable) {
        return reviewRepository.findByStatus(Status.PENDING, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public Page<ReviewResponse> getAllRejectedReviews(Pageable pageable) {
        return reviewRepository.findByStatus(Status.REJECTED, pageable)
                .map(this::mapWithShortUserInfo);
    }

    @Cacheable(
            value = "reviews-approved-user-list",
            key = "'recipient-' + #userId + '-p-' + #pageable.pageNumber + '-s-' + #pageable.pageSize + '-sort-' + #pageable.sort",
            condition = "#pageable.pageNumber == 0"
    )
    public Page<ReviewResponse> getApprovedReviewsAboutUser(Long userId, Pageable pageable) {
        return reviewRepository.findByRecipientIdAndStatus(userId, Status.APPROVED, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public Page<ReviewResponse> getUserReviews(Long userId, Pageable pageable, Status status) {
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        validateCurrentUserOrAdmin(currentKeycloakId, userId);

        return reviewRepository.findByAuthorIdAndStatus(userId, status, pageable)
                .map(this::mapWithShortUserInfo);
    }

    @CacheEvict(value = {"reviews-approved-list", "reviews-approved-user-list"}, allEntries = true)
    public void createReview(ReviewRequest reviewRequest) {
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        User currentUser = userRepository.findByKeycloakId(currentKeycloakId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, currentKeycloakId));

        User recipient = userRepository.findById(reviewRequest.getRecipientId())
                .orElseThrow(() -> new EntityNotFoundException(User.class, reviewRequest.getRecipientId()));

        if (currentUser.equals(recipient)){
            throw new InvalidRelationException("Users cannot create reviews for themselves",
                    Map.of("Recipient user id:", recipient.getId()));
        }

        if (reviewRepository.existsByAuthorIdAndRecipientId(currentUser.getId(), recipient.getId())) {
            throw new InvalidRelationException("Author has already left a review on this recipient",
                    Map.of("Author user id:", currentUser.getId(), "Recipient user id:", recipient.getId()));
        }

        Review review = reviewMapper.toEntity(reviewRequest);

        review.setAuthor(currentUser);
        review.setRecipient(recipient);
        review.setStatus(Status.PENDING);

        reviewRepository.save(review);

        log.info(
                "AUDIT review created: reviewId={}, authorId={}, recipientId={}, status={}, rating={}",
                review.getId(),
                getAuthorId(review),
                getRecipientId(review),
                review.getStatus(),
                review.getRating()
        );
    }

    @CacheEvict(value = {"reviews-approved-list", "reviews-approved-user-list"}, allEntries = true)
    public void updateReview(Long id, ReviewUpdateRequest reviewUpdateRequest) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Review.class, id));



        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();

        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && !review.getAuthor().getKeycloakId().equals(currentKeycloakId)) {
            throw new DetailedAccessDeniedException("You are not allowed to update this review",
                    Map.of(
                            "Review id:", review.getId(),
                            "Review author keycloak id:", review.getAuthor().getKeycloakId()
                    ));
        }

        review.setText(reviewUpdateRequest.getText());
        review.setRating(reviewUpdateRequest.getRating());
        review.setStatus(Status.PENDING);

        reviewRepository.save(review);

        log.info(
                "AUDIT review updated: reviewId={}, authorId={}, recipientId={}, status={}, rating={}, actorId={}",
                review.getId(),
                getAuthorId(review),
                getRecipientId(review),
                review.getStatus(),
                review.getRating(),
                currentKeycloakId
        );
    }

    // DONE add status checking (cannot approve not pending)
    @CacheEvict(value = {"reviews-approved-list", "reviews-approved-user-list"}, allEntries = true)
    public void approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Review.class, id));

        if(review.getStatus() != Status.PENDING){
            throw new InvalidStateException("Cannot change status of a non-pending review",
                    Map.of("Review id:", review.getId(), "Current status:", review.getStatus()));
        }

        review.setStatus(Status.APPROVED);

        reviewRepository.save(review);
        userService.recalculateUserRating(review.getRecipient().getId());

        log.info(
                "AUDIT review approved: reviewId={}, authorId={}, recipientId={}, status={}, actorId={}",
                review.getId(),
                getAuthorId(review),
                getRecipientId(review),
                review.getStatus(),
                SecurityUtils.getCurrentUserKeycloakId()
        );
    }

    @CacheEvict(value = {"reviews-approved-list", "reviews-approved-user-list"}, allEntries = true)
    public void rejectReview(Long id, String moderationRejectionReason) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Review.class, id));

        if(review.getStatus() != Status.PENDING){
            throw new InvalidStateException("Cannot change status of a non-pending review",
                    Map.of("Review id:", review.getId(), "Current status:", review.getStatus()));
        }

        review.setStatus(Status.REJECTED);
        review.setModerationRejectionReason(moderationRejectionReason);

        reviewRepository.save(review);

        log.info(
                "AUDIT review rejected: reviewId={}, authorId={}, recipientId={}, status={}, actorId={}",
                review.getId(),
                getAuthorId(review),
                getRecipientId(review),
                review.getStatus(),
                SecurityUtils.getCurrentUserKeycloakId()
        );
    }

    @CacheEvict(value = {"reviews-approved-list", "reviews-approved-user-list"}, allEntries = true)
    public void deleteReview(Long id) {
        if(!reviewRepository.existsById(id)){
            throw new EntityNotFoundException(Review.class, id);
        }

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Review.class, id));
        boolean wasApproved = review.getStatus() == Status.APPROVED;


        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();

        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && !review.getAuthor().getKeycloakId().equals(currentKeycloakId)) {
            throw new DetailedAccessDeniedException("You are not allowed to delete this review",
                    Map.of(
                            "Review id:", review.getId(),
                            "Review author keycloak id:", review.getAuthor().getKeycloakId()
                    ));
        }

        reviewRepository.deleteById(id);

        if (wasApproved) {
            userService.recalculateUserRating(review.getRecipient().getId());
        }

        log.info(
                "AUDIT review deleted: reviewId={}, authorId={}, recipientId={}, wasApproved={}, actorId={}",
                review.getId(),
                getAuthorId(review),
                getRecipientId(review),
                wasApproved,
                SecurityUtils.getCurrentUserKeycloakId()
        );
    }

    private void validateCurrentUserOrAdmin(String currentKeycloakId, Long userId) {
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (isAdmin) {
            return;
        }

        User requestedUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, userId));

        if (!currentKeycloakId.equals(requestedUser.getKeycloakId())) {
            throw new DetailedAccessDeniedException("You can only view your own reviews",
                    Map.of(
                            "Requested user id:", userId,
                            "Requested user keycloak id:", String.valueOf(requestedUser.getKeycloakId())
                    ));
        }
    }

    private ReviewResponse mapWithShortUserInfo(Review review){
        ReviewResponse response = reviewMapper.toResponse(review);
        response.setAuthorInfo(userResponseAssembler.toShortUserInfoResponse(review.getAuthor()));

        return response;
    }

    private Long getAuthorId(Review review) {
        return review.getAuthor() != null ? review.getAuthor().getId() : null;
    }

    private Long getRecipientId(Review review) {
        return review.getRecipient() != null ? review.getRecipient().getId() : null;
    }
}



