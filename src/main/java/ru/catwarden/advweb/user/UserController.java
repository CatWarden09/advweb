package ru.catwarden.advweb.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.ad.AdvertisementService;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.enums.Status;
import ru.catwarden.advweb.review.ReviewService;
import ru.catwarden.advweb.review.dto.ReviewResponse;
import ru.catwarden.advweb.user.dto.UserEarnedResponse;
import ru.catwarden.advweb.user.dto.UserResponse;
import ru.catwarden.advweb.user.dto.UserUpdateRequest;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AdvertisementService advertisementService;
    private final ReviewService reviewService;

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @GetMapping("/{id}/earned")
    public UserEarnedResponse getUserTotalEarned(@PathVariable Long id) {
        return userService.getUserTotalEarned(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id,
                                   @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        return userService.updateUser(id, userUpdateRequest);
    }

    @GetMapping("/{id}/advertisements")
    public Page<AdvertisementResponse> getUserApprovedAdvertisements(@PathVariable Long id,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getUserApprovedAdvertisements(id, pageable);
    }

    @GetMapping("/{id}/advertisements/pending")
    public Page<AdvertisementResponse> getUserPendingAdvertisements(@PathVariable Long id,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getUserPendingAdvertisements(id, pageable);
    }

    @GetMapping("/{id}/advertisements/rejected")
    public Page<AdvertisementResponse> getUserRejectedAdvertisements(@PathVariable Long id,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getUserRejectedAdvertisements(id, pageable);
    }

    @GetMapping("/{id}/advertisements/finished")
    public Page<AdvertisementResponse> getUserFinishedAdvertisements(@PathVariable Long id,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getUserFinishedAdvertisements(id, pageable);
    }

    @GetMapping("/{id}/reviews/received")
    public Page<ReviewResponse> getApprovedReviewsAboutUser(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getApprovedReviewsAboutUser(id, pageable);
    }

    @GetMapping("/{id}/reviews/authored/rejected")
    public Page<ReviewResponse> getUserRejectedReviews(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getUserReviews(id, pageable, Status.REJECTED);
    }

    @GetMapping("/{id}/reviews/authored/pending")
    public Page<ReviewResponse> getUserPendingReviews(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getUserReviews(id, pageable, Status.PENDING);
    }

    @GetMapping("/{id}/reviews/authored/approved")
    public Page<ReviewResponse> getUserApprovedReviews(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getUserReviews(id, pageable, Status.APPROVED);
    }

    @GetMapping("/{id}/favorites")
    public Page<AdvertisementResponse> getUserFavoriteAdvertisements(@PathVariable Long id,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getUserFavoriteAdvertisements(id, pageable);
    }

    @PutMapping("/{id}/favorites/{advertisementId}")
    public void addAdvertisementToFavorites(@PathVariable Long id, @PathVariable Long advertisementId) {
        advertisementService.addAdvertisementToFavorites(id, advertisementId);
    }

    @DeleteMapping("/{id}/favorites/{advertisementId}")
    public void removeAdvertisementFromFavorites(@PathVariable Long id, @PathVariable Long advertisementId) {
        advertisementService.removeAdvertisementFromFavorites(id, advertisementId);
    }

    @PutMapping("/{id}/avatar")
    public void setUserAvatar(@PathVariable Long id, @RequestParam Long avatarId) {
        userService.setUserAvatar(id, avatarId);
    }

    @DeleteMapping("/{id}/avatar")
    public void deleteUserAvatar(@PathVariable Long id) {
        userService.unlinkUserAvatar(id);
    }
}

