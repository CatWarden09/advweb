package ru.catwarden.advweb.review;

import org.springframework.stereotype.Component;
import ru.catwarden.advweb.review.dto.ReviewRequest;
import ru.catwarden.advweb.review.dto.ReviewResponse;

@Component
public class ReviewMapper {
    public ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .text(review.getText())
                .rating(review.getRating())
                .moderationStatus(review.getModerationStatus())
                .moderationRejectionReason(review.getModerationRejectionReason())
                .recipientId(review.getRecipient().getId())
                .createdAt(review.getCreatedAt())
                .build();
    }

    public Review toEntity(ReviewRequest reviewRequest) {
        return Review.builder()
                .text(reviewRequest.getText())
                .rating(reviewRequest.getRating())
                .build();
    }
}
