package ru.catwarden.advweb.moderation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.review.ReviewService;
import ru.catwarden.advweb.review.dto.ReviewResponse;

@RestController
@RequestMapping("/admin/reviews-moderation")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reviews Moderation", description = "Модерация отзывов")
public class ReviewModerationController {
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final ReviewService reviewService;

    @Operation(summary = "Получить отзывы на модерации")
    @GetMapping("/pending")
    public Page<ReviewResponse> getAllPendingReviews(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);

        return reviewService.getAllPendingReviews(pageable);
    }

    @Operation(summary = "Получить отклонённые отзывы")
    @GetMapping("/rejected")
    public Page<ReviewResponse> getAllRejectedReviews(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);

        return reviewService.getAllRejectedReviews(pageable);
    }

    @Operation(summary = "Одобрить отзыв")
    @PatchMapping("pending/{id}/approve")
    public void approveReview(@PathVariable Long id) {
        reviewService.approveReview(id);
    }

    @Operation(summary = "Отклонить отзыв")
    @PatchMapping("/pending/{id}/reject")
    public void rejectReview(@PathVariable Long id,
                             @RequestParam @NotBlank @Size(max = 255) String moderationRejectionReason) {
        reviewService.rejectReview(id, moderationRejectionReason);
    }


    @Operation(summary = "Удалить отзыв")
    @DeleteMapping("/{id}")
    public void deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
    }

    private Pageable buildPageable(int page, int size) {
        return PageRequest.of(page, size, DEFAULT_SORT);
    }
}
