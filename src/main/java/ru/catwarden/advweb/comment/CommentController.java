package ru.catwarden.advweb.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.comment.dto.CommentRequest;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    public void createComment(@RequestBody CommentRequest commentRequest) {
        commentService.createComment(commentRequest);
    }

    @PatchMapping("/{id}")
    public void updateComment(@PathVariable Long id, @RequestBody CommentRequest commentRequest) {
        commentService.updateComment(id, commentRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
    }

}
