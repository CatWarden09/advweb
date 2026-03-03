package ru.catwarden.advweb.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.catwarden.advweb.enums.AdModerationStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private String text;
    private Integer rating;
    private AdModerationStatus moderationStatus;
    private String moderationRejectionReason;
    private Long authorId;
    private Long recipientId;
    private LocalDateTime createdAt;
}
