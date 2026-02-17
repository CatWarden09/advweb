package ru.catwarden.advweb.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertismentRequest {

    private Long authorId;
    private String name;
    private String description;
    private Double price;
    private String address;
    private String category;
    private String subcategory;

}
