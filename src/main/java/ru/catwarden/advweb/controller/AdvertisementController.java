package ru.catwarden.advweb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.dto.request.AdvertisementRequest;
import ru.catwarden.advweb.dto.request.AdvertisementUpdateRequest;
import ru.catwarden.advweb.service.AdvertisementService;

@RestController
@RequestMapping("/advertisements")
@RequiredArgsConstructor
public class AdvertisementController {
    private final AdvertisementService advertisementService;

    @PostMapping
    void createAdvertisement(@RequestBody AdvertisementRequest advertisementRequest){
        advertisementService.createAdvertisement(advertisementRequest);
    }

    @PatchMapping("/{id}")
    void updateAdvertisement(@PathVariable Long id,
                             @RequestBody AdvertisementUpdateRequest advertisementUpdateRequest){
        advertisementService.updateAdvertisement(id, advertisementUpdateRequest);
    }

    @DeleteMapping("/{id}")
    void deleteAdvertisement(@PathVariable Long id){
        advertisementService.deleteAdvertisement(id);
    }
}
