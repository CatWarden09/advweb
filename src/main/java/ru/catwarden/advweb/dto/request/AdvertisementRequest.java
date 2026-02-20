package ru.catwarden.advweb.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.catwarden.advweb.entity.AdvertisementCategory;

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
    private AdvertisementCategory category;
    private AdvertisementCategory subcategory;

}
