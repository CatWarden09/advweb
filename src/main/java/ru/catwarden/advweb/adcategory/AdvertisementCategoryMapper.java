package ru.catwarden.advweb.adcategory;

import org.springframework.stereotype.Component;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryResponse;

@Component
public class AdvertisementCategoryMapper {
    public AdvertisementCategoryResponse toResponse(AdvertisementCategory advertisementCategory){
        return AdvertisementCategoryResponse.builder()
                .id(advertisementCategory.getId())
                .name(advertisementCategory.getName())
                .parentId(advertisementCategory.getParent() != null ? advertisementCategory.getParent().getId() : null)
                .build();
    }

    public AdvertisementCategory toEntity(AdvertisementCategoryRequest advertisementCategoryRequest){
        return AdvertisementCategory.builder()
                .name(advertisementCategoryRequest.getName())
                .build();
    }
}
