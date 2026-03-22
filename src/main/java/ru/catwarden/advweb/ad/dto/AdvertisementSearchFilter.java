package ru.catwarden.advweb.ad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementSearchFilter {
    @Size(max = 100)
    private String name;

    @Size(max = 1000)
    private String description;

    @Positive
    private Double priceMin;

    @Positive
    private  Double priceMax;

    private Long categoryId;
    private Long subcategoryId;
}
