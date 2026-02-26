package ru.catwarden.advweb.comment;

import org.springframework.stereotype.Component;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentResponse;

@Component
public class CommentMapper {
    public CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getText())
                .build();
    }

    public Comment toEntity(CommentRequest commentRequest) {
        return Comment.builder()
                .text(commentRequest.getText())
                .build();
    }
}
