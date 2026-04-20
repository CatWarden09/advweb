package ru.catwarden.advweb.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Tag(name = "Пользователи", description = "Операции с пользователями")
public class UserController {
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final UserService userService;
    private final AdvertisementService advertisementService;
    private final ReviewService reviewService;

    @Operation(summary = "Получить текущего пользователя")
    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @Operation(summary = "Получить пользователя по id")
    @GetMapping("/{id}")
    public UserResponse getUser(@Parameter(description = "ID пользователя") @PathVariable Long id) {
        return userService.getUser(id);
    }

    @Operation(summary = "Получить сумму заработка пользователя")
    @GetMapping("/{id}/earned")
    public UserEarnedResponse getUserTotalEarned(@Parameter(description = "ID пользователя") @PathVariable Long id) {
        return userService.getUserTotalEarned(id);
    }

    @Operation(summary = "Обновить пользователя")
    @PutMapping("/{id}")
    public UserResponse updateUser(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Тело запроса для обновления пользователя")
            @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        return userService.updateUser(id, userUpdateRequest);
    }

    @Operation(summary = "Получить опубликованные объявления пользователя")
    @GetMapping("/{id}/advertisements")
    public Page<AdvertisementResponse> getUserApprovedAdvertisements(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);
        return advertisementService.getUserApprovedAdvertisements(id, pageable);
    }

    @Operation(summary = "Получить объявления пользователя на модерации")
    @GetMapping("/{id}/advertisements/pending")
    public Page<AdvertisementResponse> getUserPendingAdvertisements(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);
        return advertisementService.getUserPendingAdvertisements(id, pageable);
    }

    @Operation(summary = "Получить отклонённые объявления пользователя")
    @GetMapping("/{id}/advertisements/rejected")
    public Page<AdvertisementResponse> getUserRejectedAdvertisements(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);
        return advertisementService.getUserRejectedAdvertisements(id, pageable);
    }

    @Operation(summary = "Получить завершённые объявления пользователя")
    @GetMapping("/{id}/advertisements/finished")
    public Page<AdvertisementResponse> getUserFinishedAdvertisements(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);
        return advertisementService.getUserFinishedAdvertisements(id, pageable);
    }

    @Operation(summary = "Получить подтверждённые отзывы о пользователе")
    @GetMapping("/{id}/reviews/received")
    public Page<ReviewResponse> getApprovedReviewsAboutUser(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);
        return reviewService.getApprovedReviewsAboutUser(id, pageable);
    }

    @Operation(summary = "Получить отклонённые отзывы, написанные пользователем")
    @GetMapping("/{id}/reviews/authored/rejected")
    public Page<ReviewResponse> getUserRejectedReviews(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);
        return reviewService.getUserReviews(id, pageable, Status.REJECTED);
    }

    @Operation(summary = "Получить отзывы на модерации, написанные пользователем")
    @GetMapping("/{id}/reviews/authored/pending")
    public Page<ReviewResponse> getUserPendingReviews(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size){
        Pageable pageable = buildPageable(page, size);
        return reviewService.getUserReviews(id, pageable, Status.PENDING);
    }

    @Operation(summary = "Получить подтверждённые отзывы, написанные пользователем")
    @GetMapping("/{id}/reviews/authored/approved")
    public Page<ReviewResponse> getUserApprovedReviews(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size){
        Pageable pageable = buildPageable(page, size);
        return reviewService.getUserReviews(id, pageable, Status.APPROVED);
    }

    @Operation(summary = "Получить избранные объявления пользователя")
    @GetMapping("/{id}/favorites")
    public Page<AdvertisementResponse> getUserFavoriteAdvertisements(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageableWithoutSort(page, size);
        return advertisementService.getUserFavoriteAdvertisements(id, pageable);
    }

    @Operation(summary = "Добавить объявление в избранное")
    @PutMapping("/{id}/favorites/{advertisementId}")
    public void addAdvertisementToFavorites(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "ID объявления") @PathVariable Long advertisementId) {
        advertisementService.addAdvertisementToFavorites(id, advertisementId);
    }

    @Operation(summary = "Удалить объявление из избранного")
    @DeleteMapping("/{id}/favorites/{advertisementId}")
    public void removeAdvertisementFromFavorites(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "ID объявления") @PathVariable Long advertisementId) {
        advertisementService.removeAdvertisementFromFavorites(id, advertisementId);
    }

    @Operation(summary = "Установить аватар пользователя")
    @PutMapping("/{id}/avatar")
    public void setUserAvatar(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Parameter(description = "ID аватара") @RequestParam Long avatarId) {
        userService.setUserAvatar(id, avatarId);
    }

    @Operation(summary = "Удалить аватар пользователя")
    @DeleteMapping("/{id}/avatar")
    public void deleteUserAvatar(@Parameter(description = "ID пользователя") @PathVariable Long id) {
        userService.unlinkUserAvatar(id);
    }

    private Pageable buildPageable(int page, int size) {
        return PageRequest.of(page, size, DEFAULT_SORT);
    }

    private Pageable buildPageableWithoutSort(int page, int size) {
        return PageRequest.of(page, size);
    }
}




