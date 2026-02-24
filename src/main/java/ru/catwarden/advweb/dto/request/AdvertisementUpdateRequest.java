package ru.catwarden.advweb.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementUpdateRequest {

    private String name;
    private String description;

    @Positive
    private Double price;

    private String address;

}
