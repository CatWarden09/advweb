package ru.catwarden.advweb.moderation.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.ad.AdvertisementService;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentResponse;

@RestController
@RequestMapping("/admin/ads-moderation")
@RequiredArgsConstructor
public class AdvertisementModerationController {
    private final AdvertisementService advertisementService;

    @GetMapping("/pending")
    public Page<AdvertisementResponse> getAllPendingAdvertisements(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        return advertisementService.getAllPendingAdvertisements(pageable);
    }

    @GetMapping("/rejected")
    public Page<AdvertisementResponse> getAllRejectedAdvertisements(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        return advertisementService.getAllRejectedAdvertisements(pageable);
    }

    @GetMapping("/{id}")
    public AdvertisementResponse getAdvertisement(@PathVariable Long id) {
        return advertisementService.getAdvertisement(id);
    }

    @PatchMapping("pending/{id}/approve")
    public void approveAdvertisement(@PathVariable Long id) {
        advertisementService.approveAdvertisement(id);
    }

    @PatchMapping("/pending/{id}/reject")
    public void rejectAdvertisement(@PathVariable Long id,
                                    @RequestParam @NotBlank @Size(max = 255) String moderationRejectionReason) {
        advertisementService.rejectAdvertisement(id, moderationRejectionReason);
    }


    @DeleteMapping("/{id}")
    public void deleteAdvertisement(@PathVariable Long id) {
        advertisementService.deleteAdvertisement(id);
    }
}