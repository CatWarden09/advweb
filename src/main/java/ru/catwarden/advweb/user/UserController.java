package ru.catwarden.advweb.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.ad.AdvertisementService;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.review.ReviewService;
import ru.catwarden.advweb.review.dto.ReviewResponse;
import ru.catwarden.advweb.user.dto.UserResponse;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AdvertisementService advertisementService;
    private final ReviewService reviewService;

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @GetMapping("/{id}/advertisements")
    public Page<AdvertisementResponse> getUserApprovedAdvertisements(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getUserApprovedAdvertisements(id, pageable);
    }

    // TODO add authentication
    @GetMapping("/{id}/advertisements/pending")
    public Page<AdvertisementResponse> getUserPendingAdvertisements(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getUserPendingAdvertisements(id, pageable);
    }

    @GetMapping("/{id}/advertisements/rejected")
    public Page<AdvertisementResponse> getUserRejectedAdvertisements(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getUserRejectedAdvertisements(id, pageable);
    }

    @GetMapping("/{id}/reviews/received")
    public Page<ReviewResponse> getApprovedReviewsAboutUser(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getApprovedReviewsAboutUser(id, pageable);
    }

    // TODO add authentication
    @GetMapping("/{id}/reviews/authored/rejected")
    public Page<ReviewResponse> getUserRejectedReviews(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getUserRejectedReviews(id, pageable);
    }

    @GetMapping("/{id}/reviews/authored/pending")
    public Page<ReviewResponse> getUserPendingReviews(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getUserPendingReviews(id, pageable);
    }

    @GetMapping("/{id}/reviews/authored/approved")
    public Page<ReviewResponse> getUserApprovedReviews(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getUserApprovedReviews(id, pageable);
    }
}
