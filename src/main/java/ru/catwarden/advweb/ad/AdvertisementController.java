package ru.catwarden.advweb.ad;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.ad.dto.AdvertisementSearchFilter;
import ru.catwarden.advweb.ad.dto.AdvertisementUpdateRequest;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.comment.dto.CommentResponse;



@RestController
@RequestMapping("/advertisements")
@RequiredArgsConstructor
@Validated
public class AdvertisementController {
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final AdvertisementService advertisementService;
    private final CommentService commentService;

    @GetMapping("/{id}")
    public AdvertisementResponse getAdvertisement(@PathVariable Long id){
        AdvertisementResponse response = advertisementService.getAdvertisement(id);

        return response;
    }

    @GetMapping
    public Page<AdvertisementResponse> getAllApprovedAdvertisements(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size){
        Pageable pageable = buildPageable(page, size);
        return advertisementService.getAllApprovedAdvertisements(pageable);

    }


    @GetMapping("/{id}/comments")
    public Page<CommentResponse> getAdvertisementComments(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size){
        Pageable pageable = buildPageable(page, size);
        return commentService.getAdvertisementModeratedComments(id, pageable);
    }

    @PostMapping("/search")
    public Page<AdvertisementResponse> getAdvertisementsByFilter(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size,
                                                                 @Valid @RequestBody AdvertisementSearchFilter filter){
        Pageable pageable = buildPageable(page, size);
        return advertisementService.getAdvertisementsByFilter(pageable, filter);
    }

    @PostMapping
    public Long createAdvertisement(@Valid @RequestBody AdvertisementRequest advertisementRequest){
        return advertisementService.createAdvertisement(advertisementRequest);
    }

    @PutMapping("/{id}/update")
    public void updateAdvertisement(@PathVariable Long id,
                                    @Valid @RequestBody AdvertisementUpdateRequest advertisementUpdateRequest){
        advertisementService.updateAdvertisement(id, advertisementUpdateRequest);
    }

    @PutMapping("/{id}/finish")
    public void finishAdvertisement(@PathVariable Long id){
        advertisementService.finishAdvertisement(id);
    }

    @DeleteMapping("/{id}")
    public void deleteAdvertisement(@PathVariable Long id){
        advertisementService.deleteAdvertisement(id);
    }

    private Pageable buildPageable(int page, int size) {
        return PageRequest.of(page, size, DEFAULT_SORT);
    }

}
