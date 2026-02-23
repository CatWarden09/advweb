package ru.catwarden.advweb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.dto.response.AdvertisementResponse;
import ru.catwarden.advweb.service.AdvertisementService;

@RestController
@RequestMapping("/admin/moderation")
@RequiredArgsConstructor
public class ModerationController {
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
    public AdvertisementResponse getAdvertisement(@PathVariable Long id){
        return advertisementService.getAdvertisement(id);
    }

    @PatchMapping("/pending/{id}/approve")
    public void approveAdvertisement(@PathVariable Long id){
        advertisementService.approveAdvertisement(id);
    }

    @PatchMapping("/pending/{id}/reject")
    public void rejectAdvertisement(@PathVariable Long id,
                                    @RequestParam String moderationRejectionReason){
        advertisementService.rejectAdvertisement(id, moderationRejectionReason);
    }


    @DeleteMapping("/{id}")
    public void deleteAdvertisement(@PathVariable Long id){
        advertisementService.deleteAdvertisement(id);
    }
}
