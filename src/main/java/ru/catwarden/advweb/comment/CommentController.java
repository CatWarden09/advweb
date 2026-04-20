package ru.catwarden.advweb.comment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentUpdateRequest;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Комментарии", description = "Операции с комментариями")
public class CommentController {
    private final CommentService commentService;

    @Operation(summary = "Создать комментарий")
    @PostMapping
    public void createComment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Тело запроса для создания комментария")
            @Valid @RequestBody CommentRequest commentRequest) {
        commentService.createComment(commentRequest);
    }

    @Operation(summary = "Обновить комментарий")
    @PatchMapping("/{id}")
    public void updateComment(
            @Parameter(description = "ID комментария") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Тело запроса для обновления комментария")
            @Valid @RequestBody CommentUpdateRequest commentUpdateRequest) {
        commentService.updateComment(id, commentUpdateRequest);
    }

    @Operation(summary = "Удалить комментарий")
    @DeleteMapping("/{id}")
    public void deleteComment(@Parameter(description = "ID комментария") @PathVariable Long id) {
        commentService.deleteComment(id);
    }

}


