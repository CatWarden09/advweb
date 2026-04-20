package ru.catwarden.advweb.moderation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.ad.AdvertisementService;

@RestController
@RequestMapping("/admin/ads-moderation")
@RequiredArgsConstructor
@Validated
@Tag(name = "Модерация объявлений", description = "Модерация объявлений")
public class AdvertisementModerationController {
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final AdvertisementService advertisementService;

    @Operation(summary = "Получить объявления на модерации")
    @GetMapping("/pending")
    public Page<AdvertisementResponse> getAllPendingAdvertisements(
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);

        return advertisementService.getAllPendingAdvertisements(pageable);
    }

    @Operation(summary = "Получить отклонённые объявления")
    @GetMapping("/rejected")
    public Page<AdvertisementResponse> getAllRejectedAdvertisements(
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);

        return advertisementService.getAllRejectedAdvertisements(pageable);
    }

    @Operation(summary = "Одобрить объявление")
    @PatchMapping("pending/{id}/approve")
    public void approveAdvertisement(@Parameter(description = "ID объявления") @PathVariable Long id) {
        advertisementService.approveAdvertisement(id);
    }

    @Operation(summary = "Отклонить объявление")
    @PatchMapping("/pending/{id}/reject")
    public void rejectAdvertisement(
            @Parameter(description = "ID объявления") @PathVariable Long id,
            @Parameter(description = "Причина отклонения модератором")
            @RequestParam @NotBlank @Size(max = 255) String moderationRejectionReason) {
        advertisementService.rejectAdvertisement(id, moderationRejectionReason);
    }

    private Pageable buildPageable(int page, int size) {
        return PageRequest.of(page, size, DEFAULT_SORT);
    }
}


