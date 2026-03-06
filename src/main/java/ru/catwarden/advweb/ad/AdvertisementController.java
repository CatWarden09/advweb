package ru.catwarden.advweb.ad;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementUpdateRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.comment.Comment;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.comment.dto.CommentResponse;
import ru.catwarden.advweb.image.ImageService;

import java.util.List;


// TODO add default redirect (@GlobalExceptionHandler...)
@RestController
@RequestMapping("/advertisements")
@RequiredArgsConstructor
@Validated
public class AdvertisementController {
    private final AdvertisementService advertisementService;
    private final CommentService commentService;

    @GetMapping("/{id}")
    public AdvertisementResponse getAdvertisement(@PathVariable Long id){
        return advertisementService.getAdvertisement(id);
    }

    @GetMapping
    public Page<AdvertisementResponse> getAllApprovedAdvertisements(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return advertisementService.getAllApprovedAdvertisements(pageable);

    }

    @GetMapping("/{id}/comments")
    public Page<CommentResponse> getAdvertisementComments(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return commentService.getAdvertisementModeratedComments(id, pageable);
    }

    @PostMapping
    public Long createAdvertisement(@Valid @RequestBody AdvertisementRequest advertisementRequest){
        return advertisementService.createAdvertisement(advertisementRequest);
    }

    @PatchMapping("/{id}")
    public void updateAdvertisement(@PathVariable Long id,
                                    @Valid @RequestBody AdvertisementUpdateRequest advertisementUpdateRequest){
        advertisementService.updateAdvertisement(id, advertisementUpdateRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteAdvertisement(@PathVariable Long id){
        advertisementService.deleteAdvertisement(id);
    }


}
