package ru.catwarden.advweb.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.catwarden.advweb.review.dto.ReviewRequest;
import ru.catwarden.advweb.review.dto.ReviewUpdateRequest;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    private ReviewController reviewController;

    @BeforeEach
    void setUp() {
        reviewController = new ReviewController(reviewService);
    }

    @Test
    void createReviewDelegatesToService() {
        ReviewRequest request = ReviewRequest.builder()
                .recipientId(1L)
                .text("This review text is long enough and meaningful for controller test delegation.")
                .rating(5)
                .build();

        reviewController.createReview(request);

        verify(reviewService).createReview(request);
    }

    @Test
    void updateReviewDelegatesToService() {
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .text("This updated review text is long enough and meaningful for controller delegation.")
                .rating(4)
                .build();

        reviewController.updateReview(10L, request);

        verify(reviewService).updateReview(10L, request);
    }

    @Test
    void deleteReviewDelegatesToService() {
        reviewController.deleteReview(8L);

        verify(reviewService).deleteReview(8L);
    }
}

