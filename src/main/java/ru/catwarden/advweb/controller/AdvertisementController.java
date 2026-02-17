package ru.catwarden.advweb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.catwarden.advweb.dto.request.AdvertismentRequest;
import ru.catwarden.advweb.service.AdvertisementService;

@RestController
@RequestMapping("/advertisements")
@RequiredArgsConstructor
public class AdvertisementController {
    private final AdvertisementService advertisementService;

    @PostMapping
    void createAdv(@RequestBody AdvertismentRequest advertismentRequest){
        advertisementService.createAdvertisement(advertismentRequest);
    }
}
