package ru.catwarden.advweb.review.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateRequest {
    @NotBlank
    @Size(min = 50, max = 300)
    private String text;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer rating;
}
