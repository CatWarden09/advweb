package ru.catwarden.advweb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.dto.request.AdvertisementRequest;
import ru.catwarden.advweb.dto.request.AdvertisementUpdateRequest;
import ru.catwarden.advweb.dto.response.AdvertisementResponse;
import ru.catwarden.advweb.service.AdvertisementService;


// TODO add default redirect (@ExceptionHandler...)
@RestController
@RequestMapping("/advertisements")
@RequiredArgsConstructor
public class AdvertisementController {
    private final AdvertisementService advertisementService;

    @GetMapping("/{id}")
    public AdvertisementResponse getAdvertisement(@PathVariable Long id){
        return advertisementService.getAdvertisement(id);
    }

    @GetMapping("/page")
    public Page<AdvertisementResponse> getAllAdvertisements(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getAllAdvertisements(pageable);
    }

    @PostMapping
    public void createAdvertisement(@RequestBody AdvertisementRequest advertisementRequest){
        advertisementService.createAdvertisement(advertisementRequest);
    }

    @PatchMapping("/{id}")
    public void updateAdvertisement(@PathVariable Long id,
                             @RequestBody AdvertisementUpdateRequest advertisementUpdateRequest){
        advertisementService.updateAdvertisement(id, advertisementUpdateRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteAdvertisement(@PathVariable Long id){
        advertisementService.deleteAdvertisement(id);
    }
}
