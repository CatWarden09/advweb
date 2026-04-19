package ru.catwarden.advweb.moderation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentResponse;

@RestController
@RequestMapping("/admin/comments-moderation")
@RequiredArgsConstructor
@Validated
public class CommentModerationController {
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final CommentService commentService;

    @GetMapping
    public Page<CommentResponse> getAllUnmoderatedComments(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);

        return commentService.getAllUnmoderatedComments(pageable);
    }

    @PatchMapping("/{id}")
    public void updateCommentOnModeration(@PathVariable Long id,
                                          @Valid @RequestBody CommentRequest commentRequest) {
        commentService.updateCommentOnModeration(id, commentRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
    }

    private Pageable buildPageable(int page, int size) {
        return PageRequest.of(page, size, DEFAULT_SORT);
    }
}



