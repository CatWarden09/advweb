package ru.catwarden.advweb.ad.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementUpdateRequest {
    @NotBlank
    @Size(min = 5, max = 100)
    private String name;

    @NotBlank
    @Size(min = 10, max = 1000)
    private String description;

    @NotNull
    @Positive
    private Double price;

    @Valid
    @NotNull
    private AddressDto address;

    @NotEmpty
    private List<Long> imageIds;

}
