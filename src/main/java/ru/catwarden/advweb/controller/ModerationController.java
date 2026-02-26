package ru.catwarden.advweb.controller;

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
@RequestMapping("/admin/moderation")
@RequiredArgsConstructor
public class ModerationController {
    private final AdvertisementService advertisementService;
    private final CommentService commentService;

    @GetMapping("/ads/pending")
    public Page<AdvertisementResponse> getAllPendingAdvertisements(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        return advertisementService.getAllPendingAdvertisements(pageable);
    }

    @GetMapping("/ads/rejected")
    public Page<AdvertisementResponse> getAllRejectedAdvertisements(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        return advertisementService.getAllRejectedAdvertisements(pageable);
    }

    @GetMapping("/ads/{id}")
    public AdvertisementResponse getAdvertisement(@PathVariable Long id){
        return advertisementService.getAdvertisement(id);
    }

    @PatchMapping("/ads/pending/{id}/approve")
    public void approveAdvertisement(@PathVariable Long id){
        advertisementService.approveAdvertisement(id);
    }

    @PatchMapping("/ads/pending/{id}/reject")
    public void rejectAdvertisement(@PathVariable Long id,
                                    @RequestParam String moderationRejectionReason){
        advertisementService.rejectAdvertisement(id, moderationRejectionReason);
    }


    @DeleteMapping("/ads/{id}")
    public void deleteAdvertisement(@PathVariable Long id){
        advertisementService.deleteAdvertisement(id);
    }

    @GetMapping("/comments")
    public Page<CommentResponse> getAllComments(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        return commentService.getAllUnmoderatedComments(pageable);
    }

    @PatchMapping("/comments/{id}")
    public void updateCommentOnModeration(@PathVariable Long id, @RequestBody CommentRequest commentRequest) {
        commentService.updateCommentOnModeration(id, commentRequest);
    }

    @DeleteMapping("/comments/{id}")
    public void deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
    }
}
