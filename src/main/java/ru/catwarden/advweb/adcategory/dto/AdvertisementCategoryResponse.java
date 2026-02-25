package ru.catwarden.advweb.adcategory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementCategoryResponse {
    private Long id;
    private String name;
    private Long parentId;

}
