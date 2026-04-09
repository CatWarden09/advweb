package ru.catwarden.advweb.moderation.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentModerationControllerTest {

    @Mock
    private CommentService commentService;

    private CommentModerationController controller;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() {
        controller = new CommentModerationController(commentService);
    }

    @Test
    void getAllUnmoderatedCommentsPassesCorrectPageable() {
        Page<CommentResponse> page = new PageImpl<>(List.of(CommentResponse.builder().id(3L).build()));
        when(commentService.getAllUnmoderatedComments(any(Pageable.class))).thenReturn(page);

        Page<CommentResponse> result = controller.getAllUnmoderatedComments(2, 15);

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(commentService).getAllUnmoderatedComments(captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(15, captor.getValue().getPageSize());
    }

    @Test
    void updateCommentOnModerationDelegatesToService() {
        CommentRequest request = CommentRequest.builder()
                .advertisementId(10L)
                .text("This comment text is long enough for moderation update unit test coverage.")
                .build();

        controller.updateCommentOnModeration(4L, request);

        verify(commentService).updateCommentOnModeration(4L, request);
    }

    @Test
    void deleteCommentDelegatesToService() {
        controller.deleteComment(8L);

        verify(commentService).deleteComment(8L);
    }

    @Test
    void updateCommentOnModerationValidatesRequestBody() throws NoSuchMethodException {
        Method method = CommentModerationController.class.getMethod(
                "updateCommentOnModeration",
                Long.class,
                CommentRequest.class
        );
        CommentRequest invalidRequest = CommentRequest.builder()
                .advertisementId(null)
                .text("abc")
                .build();

        Set<ConstraintViolation<CommentModerationController>> violations =
                validator.forExecutables().validateParameters(controller, method, new Object[]{5L, invalidRequest});

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v ->
                v.getConstraintDescriptor().getAnnotation().annotationType().equals(NotNull.class)));
        assertTrue(violations.stream().anyMatch(v ->
                v.getConstraintDescriptor().getAnnotation().annotationType().equals(Size.class)));
    }
}
