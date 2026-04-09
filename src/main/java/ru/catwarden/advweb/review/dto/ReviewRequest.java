package ru.catwarden.advweb.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    @NotNull
    private Long recipientId;

    @Size(min = 30, max = 300)
    private String text;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer rating;

}
