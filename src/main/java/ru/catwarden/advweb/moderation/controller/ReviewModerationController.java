package ru.catwarden.advweb.moderation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.review.ReviewService;
import ru.catwarden.advweb.review.dto.ReviewResponse;

@RestController
@RequestMapping("/admin/reviews-moderation")
@RequiredArgsConstructor
public class ReviewModerationController {
    private final ReviewService reviewService;
    
    @GetMapping("/pending")
    public Page<ReviewResponse> getAllPendingReviews(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        return reviewService.getAllPendingReviews(pageable);
    }

    @GetMapping("/rejected")
    public Page<ReviewResponse> getAllRejectedReviews(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        return reviewService.getAllRejectedReviews(pageable);
    }

    @PatchMapping("pending/{id}/approve")
    public void approveReview(@PathVariable Long id) {
        reviewService.approveReview(id);
    }

    @PatchMapping("/pending/{id}/reject")
    public void rejectReview(@PathVariable Long id,
                                    @RequestParam String moderationRejectionReason) {
        reviewService.rejectReview(id, moderationRejectionReason);
    }


    @DeleteMapping("/{id}")
    public void deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
    }
}
