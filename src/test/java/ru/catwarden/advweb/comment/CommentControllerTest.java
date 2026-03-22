package ru.catwarden.advweb.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentUpdateRequest;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    private CommentController commentController;

    @BeforeEach
    void setUp() {
        commentController = new CommentController(commentService);
    }

    @Test
    void createCommentDelegatesToService() {
        CommentRequest request = CommentRequest.builder()
                .advertisementId(1L)
                .text("This is a long enough comment text to satisfy validation requirements.")
                .build();

        commentController.createComment(request);

        verify(commentService).createComment(request);
    }

    @Test
    void updateCommentDelegatesToService() {
        CommentUpdateRequest request = CommentUpdateRequest.builder()
                .text("Updated comment text that is also long enough for validation rules.")
                .build();

        commentController.updateComment(5L, request);

        verify(commentService).updateComment(5L, request);
    }

    @Test
    void deleteCommentDelegatesToService() {
        commentController.deleteComment(8L);

        verify(commentService).deleteComment(8L);
    }
}

