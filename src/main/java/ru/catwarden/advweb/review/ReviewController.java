package ru.catwarden.advweb.review;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.review.dto.ReviewRequest;
import ru.catwarden.advweb.review.dto.ReviewUpdateRequest;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public void createReview(@Valid @RequestBody ReviewRequest reviewRequest) {
        reviewService.createReview(reviewRequest);
    }

    @PatchMapping("/{id}")
    public void updateReview(@PathVariable Long id,
                             @Valid @RequestBody ReviewUpdateRequest reviewUpdateRequest) {
        reviewService.updateReview(id, reviewUpdateRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
    }

}
