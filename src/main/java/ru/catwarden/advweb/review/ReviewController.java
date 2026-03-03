package ru.catwarden.advweb.review;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentUpdateRequest;
import ru.catwarden.advweb.review.dto.ReviewRequest;
import ru.catwarden.advweb.review.dto.ReviewUpdateRequest;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public void createReview(@RequestBody ReviewRequest reviewRequest) {
        reviewService.createReview(reviewRequest);
    }

    @PatchMapping("/{id}")
    public void updateReview(@PathVariable Long id, @RequestBody ReviewUpdateRequest reviewUpdateRequest) {
        reviewService.updateReview(id, reviewUpdateRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
    }

}
