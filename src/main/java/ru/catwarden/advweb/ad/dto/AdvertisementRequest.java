package ru.catwarden.advweb.ad.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementRequest {
    private Long authorId;
    private String name;
    private String description;

    @Positive
    private Double price;

    private String address;
    private Long categoryId;
    private Long subcategoryId;

    private List<Long> imageIds;
}
