package ru.catwarden.advweb.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.catwarden.advweb.user.dto.ShortUserInfoResponse;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long adId;
    private String text;
    private Boolean isModerated;
    private ShortUserInfoResponse authorInfo;
    private LocalDateTime createdAt;
}
