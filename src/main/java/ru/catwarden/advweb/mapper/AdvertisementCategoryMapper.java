package ru.catwarden.advweb.mapper;

import org.springframework.stereotype.Component;
import ru.catwarden.advweb.dto.request.AdvertisementCategoryRequest;
import ru.catwarden.advweb.dto.response.AdvertisementCategoryResponse;
import ru.catwarden.advweb.entity.AdvertisementCategory;

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
