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
public class AdvertisementRequest {


    private Long authorId;
    private String name;
    private String description;

    @Positive
    private Double price;

    private String address;
    private Long categoryId;
    private Long subcategoryId;

}
