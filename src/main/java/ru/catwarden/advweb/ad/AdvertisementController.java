package ru.catwarden.advweb.ad;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementUpdateRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.image.ImageService;

import java.util.List;


// TODO add default redirect (@ExceptionHandler...)
@RestController
@RequestMapping("/advertisements")
@RequiredArgsConstructor
public class AdvertisementController {
    private final AdvertisementService advertisementService;

    @GetMapping("/{id}")
    public AdvertisementResponse getAdvertisement(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);

        return advertisementService.getAdvertisement(id, pageable);
    }

    @GetMapping
    public Page<AdvertisementResponse> getAllApprovedAdvertisements(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getAllApprovedAdvertisements(pageable);
    }

    @PostMapping
    public Long createAdvertisement(@RequestBody AdvertisementRequest advertisementRequest){
        return advertisementService.createAdvertisement(advertisementRequest);
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
