package ru.catwarden.advweb.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    private Long authorId;
    private Long recipientId;
    private String text;
    private Integer rating;

}
