package ru.catwarden.advweb.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.catwarden.advweb.enums.Status;
import ru.catwarden.advweb.user.dto.ShortUserInfoResponse;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private String text;
    private Integer rating;
    private Status status;
    private String moderationRejectionReason;
    private ShortUserInfoResponse authorInfo;
    private Long recipientId;
    private LocalDateTime createdAt;
}
