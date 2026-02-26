package ru.catwarden.advweb.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

}
