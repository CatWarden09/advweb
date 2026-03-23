package ru.catwarden.advweb.moderation.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.catwarden.advweb.review.ReviewService;
import ru.catwarden.advweb.review.dto.ReviewResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewModerationControllerTest {

    @Mock
    private ReviewService reviewService;

    private ReviewModerationController controller;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() {
        controller = new ReviewModerationController(reviewService);
    }

    @Test
    void getAllPendingReviewsPassesCorrectPageable() {
        Page<ReviewResponse> page = new PageImpl<>(List.of(ReviewResponse.builder().id(1L).build()));
        when(reviewService.getAllPendingReviews(any(Pageable.class))).thenReturn(page);

        Page<ReviewResponse> result = controller.getAllPendingReviews(0, 20);

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService).getAllPendingReviews(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    void getAllRejectedReviewsPassesCorrectPageable() {
        Page<ReviewResponse> page = new PageImpl<>(List.of(ReviewResponse.builder().id(2L).build()));
        when(reviewService.getAllRejectedReviews(any(Pageable.class))).thenReturn(page);

        Page<ReviewResponse> result = controller.getAllRejectedReviews(3, 5);

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService).getAllRejectedReviews(captor.capture());
        assertEquals(3, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void approveReviewDelegatesToService() {
        controller.approveReview(6L);

        verify(reviewService).approveReview(6L);
    }

    @Test
    void rejectReviewDelegatesToService() {
        controller.rejectReview(6L, "Contains prohibited language");

        verify(reviewService).rejectReview(6L, "Contains prohibited language");
    }

    @Test
    void deleteReviewDelegatesToService() {
        controller.deleteReview(7L);

        verify(reviewService).deleteReview(7L);
    }

    @Test
    void rejectReviewHasValidationForBlankReason() throws NoSuchMethodException {
        Method method = ReviewModerationController.class.getMethod(
                "rejectReview",
                Long.class,
                String.class
        );

        Set<ConstraintViolation<ReviewModerationController>> violations =
                validator.forExecutables().validateParameters(controller, method, new Object[]{1L, ""});

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v ->
                v.getConstraintDescriptor().getAnnotation().annotationType().equals(NotBlank.class)));
    }

    @Test
    void rejectReviewHasValidationForTooLongReason() throws NoSuchMethodException {
        Method method = ReviewModerationController.class.getMethod(
                "rejectReview",
                Long.class,
                String.class
        );
        String longReason = "b".repeat(256);

        Set<ConstraintViolation<ReviewModerationController>> violations =
                validator.forExecutables().validateParameters(controller, method, new Object[]{1L, longReason});

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v ->
                v.getConstraintDescriptor().getAnnotation().annotationType().equals(Size.class)));
    }
}
