package ru.catwarden.advweb.review;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.InvalidStateException;
import ru.catwarden.advweb.review.dto.ReviewRequest;
import ru.catwarden.advweb.review.dto.ReviewResponse;
import ru.catwarden.advweb.review.dto.ReviewUpdateRequest;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserMapper;
import ru.catwarden.advweb.user.UserRepository;

// DONE users cant leave reviews for themselves

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

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

    public Page<ReviewResponse> getUserApprovedReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByAuthorIdAndModerationStatus(userId, AdModerationStatus.APPROVED, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public Page<ReviewResponse> getUserPendingReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByAuthorIdAndModerationStatus(userId, AdModerationStatus.PENDING, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public Page<ReviewResponse> getUserRejectedReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByAuthorIdAndModerationStatus(userId, AdModerationStatus.REJECTED, pageable)
                .map(this::mapWithShortUserInfo);
    }

    public void createReview(ReviewRequest reviewRequest) {
        User author = userRepository.findById(reviewRequest.getAuthorId())
                .orElseThrow(() -> new EntityNotFoundException(User.class, reviewRequest.getAuthorId()));
        User recipient = userRepository.findById(reviewRequest.getRecipientId())
                .orElseThrow(() -> new EntityNotFoundException(User.class, reviewRequest.getAuthorId()));

        if (author.equals(recipient)){
            throw new RuntimeException("Users cannot create reviews for themselves");
        }

        Review review = reviewMapper.toEntity(reviewRequest);

        review.setAuthor(author);
        review.setRecipient(recipient);
        review.setModerationStatus(AdModerationStatus.PENDING);

        reviewRepository.save(review);
    }

    public void updateReview(Long id, ReviewUpdateRequest reviewUpdateRequest) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Review.class, id));

        review.setText(reviewUpdateRequest.getText());
        review.setRating(reviewUpdateRequest.getRating());
        review.setModerationStatus(AdModerationStatus.PENDING);

        reviewRepository.save(review);
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
    }

    public void rejectReview(Long id, String moderationRejectionReason) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Review.class, id));

        if(review.getModerationStatus() != AdModerationStatus.PENDING){
            throw new InvalidStateException("Cannot change status of a non-pending review");
        }

        review.setModerationStatus(AdModerationStatus.REJECTED);
        review.setModerationRejectionReason(review.getModerationRejectionReason());

        reviewRepository.save(review);
    }

    public void deleteReview(Long id) {
        if(!reviewRepository.existsById(id)){
            throw new EntityNotFoundException(Review.class, id);
        }
        reviewRepository.deleteById(id);
    }

    private ReviewResponse mapWithShortUserInfo(Review review){
        ReviewResponse response = reviewMapper.toResponse(review);
        response.setAuthorInfo(userMapper.toShortUserInfoResponse(review.getAuthor()));

        return response;
    }
}
