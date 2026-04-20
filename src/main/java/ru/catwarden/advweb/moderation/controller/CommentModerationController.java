package ru.catwarden.advweb.moderation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Comments Moderation", description = "Модерация комментариев")
public class CommentModerationController {
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final CommentService commentService;

    @Operation(summary = "Получить комментарии на модерации")
    @GetMapping
    public Page<CommentResponse> getAllUnmoderatedComments(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = buildPageable(page, size);

        return commentService.getAllUnmoderatedComments(pageable);
    }

    @Operation(summary = "Изменить комментарий на модерации")
    @PatchMapping("/{id}")
    public void updateCommentOnModeration(@PathVariable Long id,
                                          @Valid @RequestBody CommentRequest commentRequest) {
        commentService.updateCommentOnModeration(id, commentRequest);
    }

    @Operation(summary = "Удалить комментарий")
    @DeleteMapping("/{id}")
    public void deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
    }

    private Pageable buildPageable(int page, int size) {
        return PageRequest.of(page, size, DEFAULT_SORT);
    }
}



