package ru.catwarden.advweb.comment.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CommentRequest {
    @NotNull
    private Long authorId;

    @NotNull
    private Long advertisementId;

    @NotBlank
    @Size(min = 50, max = 300)
    private String text;
}
