package ru.catwarden.advweb.review;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.review.dto.ReviewRequest;
import ru.catwarden.advweb.review.dto.ReviewResponse;
import ru.catwarden.advweb.review.dto.ReviewUpdateRequest;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserRepository;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    private final ReviewMapper reviewMapper;

    public Page<ReviewResponse> getAllApprovedReviews(Pageable pageable) {
        return reviewRepository.findByModerationStatus(AdModerationStatus.APPROVED, pageable)
                .map(reviewMapper::toResponse);
    }

    public Page<ReviewResponse> getAllPendingReviews(Pageable pageable) {
        return reviewRepository.findByModerationStatus(AdModerationStatus.PENDING, pageable)
                .map(reviewMapper::toResponse);
    }

    public Page<ReviewResponse> getAllRejectedReviews(Pageable pageable) {
        return reviewRepository.findByModerationStatus(AdModerationStatus.REJECTED, pageable)
                .map(reviewMapper::toResponse);
    }

    public void createReview(ReviewRequest reviewRequest) {
        User author = userRepository.findById(reviewRequest.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Author not found"));
        User recipient = userRepository.findById(reviewRequest.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        Review review = reviewMapper.toEntity(reviewRequest);

        review.setAuthor(author);
        review.setRecipient(recipient);
        review.setModerationStatus(AdModerationStatus.PENDING);

        reviewRepository.save(review);
    }

    public void updateReview(Long id, ReviewUpdateRequest reviewUpdateRequest) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setText(reviewUpdateRequest.getText());
        review.setRating(reviewUpdateRequest.getRating());
        review.setModerationStatus(AdModerationStatus.PENDING);

        reviewRepository.save(review);
    }

    // TODO add status checking (cannot approve not pending)
    public void approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setModerationStatus(AdModerationStatus.APPROVED);

        reviewRepository.save(review);
    }

    public void rejectReview(Long id, String moderationRejectionReason) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setModerationStatus(AdModerationStatus.REJECTED);
        review.setModerationRejectionReason(review.getModerationRejectionReason());

        reviewRepository.save(review);
    }

    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }
}
