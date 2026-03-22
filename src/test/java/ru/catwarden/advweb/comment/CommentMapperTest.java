package ru.catwarden.advweb.comment;

import org.junit.jupiter.api.Test;
import ru.catwarden.advweb.ad.Advertisement;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentResponse;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommentMapperTest {

    private final CommentMapper commentMapper = new CommentMapper();

    @Test
    void toResponseMapsMainFields() {
        Advertisement advertisement = Advertisement.builder().id(44L).build();
        LocalDateTime createdAt = LocalDateTime.now();
        Comment comment = Comment.builder()
                .id(10L)
                .ad(advertisement)
                .text("Very useful and detailed feedback text for this advertisement offer.")
                .isModerated(true)
                .createdAt(createdAt)
                .build();

        CommentResponse result = commentMapper.toResponse(comment);

        assertEquals(10L, result.getId());
        assertEquals(44L, result.getAdId());
        assertEquals("Very useful and detailed feedback text for this advertisement offer.", result.getText());
        assertEquals(true, result.getIsModerated());
        assertEquals(createdAt, result.getCreatedAt());
    }

    @Test
    void toEntitySetsTextAndDefaultModerationFlag() {
        CommentRequest request = CommentRequest.builder()
                .advertisementId(44L)
                .text("This is a long enough comment text to satisfy validation requirements.")
                .build();

        Comment result = commentMapper.toEntity(request);

        assertEquals("This is a long enough comment text to satisfy validation requirements.", result.getText());
        assertFalse(result.getIsModerated());
    }
}

