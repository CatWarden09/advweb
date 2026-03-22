package ru.catwarden.advweb.review;

import org.junit.jupiter.api.Test;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.review.dto.ReviewRequest;
import ru.catwarden.advweb.review.dto.ReviewResponse;
import ru.catwarden.advweb.user.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviewMapperTest {

    private final ReviewMapper reviewMapper = new ReviewMapper();

    @Test
    void toResponseMapsMainFields() {
        User recipient = User.builder().id(15L).build();
        LocalDateTime createdAt = LocalDateTime.now();
        Review review = Review.builder()
                .id(7L)
                .text("This review text is long enough and meaningful for mapping check in tests.")
                .rating(5)
                .moderationStatus(AdModerationStatus.APPROVED)
                .moderationRejectionReason(null)
                .recipient(recipient)
                .createdAt(createdAt)
                .build();

        ReviewResponse result = reviewMapper.toResponse(review);

        assertEquals(7L, result.getId());
        assertEquals("This review text is long enough and meaningful for mapping check in tests.", result.getText());
        assertEquals(5, result.getRating());
        assertEquals(AdModerationStatus.APPROVED, result.getModerationStatus());
        assertEquals(15L, result.getRecipientId());
        assertEquals(createdAt, result.getCreatedAt());
    }

    @Test
    void toEntityMapsTextAndRating() {
        ReviewRequest request = ReviewRequest.builder()
                .recipientId(15L)
                .text("This review text is long enough and meaningful for mapping check in tests.")
                .rating(4)
                .build();

        Review result = reviewMapper.toEntity(request);

        assertEquals("This review text is long enough and meaningful for mapping check in tests.", result.getText());
        assertEquals(4, result.getRating());
    }
}

