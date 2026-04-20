package ru.catwarden.advweb.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Отзывы", description = "Операции с отзывами")
public class ReviewController {
    private final ReviewService reviewService;

    @Operation(summary = "Создать отзыв")
    @PostMapping
    public void createReview(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Тело запроса для создания отзыва")
            @Valid @RequestBody ReviewRequest reviewRequest) {
        reviewService.createReview(reviewRequest);
    }

    @Operation(summary = "Обновить отзыв")
    @PatchMapping("/{id}")
    public void updateReview(
            @Parameter(description = "ID отзыва") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Тело запроса для обновления отзыва")
            @Valid @RequestBody ReviewUpdateRequest reviewUpdateRequest) {
        reviewService.updateReview(id, reviewUpdateRequest);
    }

    @Operation(summary = "Удалить отзыв")
    @DeleteMapping("/{id}")
    public void deleteReview(@Parameter(description = "ID отзыва") @PathVariable Long id) {
        reviewService.deleteReview(id);
    }

}


