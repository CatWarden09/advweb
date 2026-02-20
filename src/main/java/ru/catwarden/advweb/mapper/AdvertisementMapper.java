package ru.catwarden.advweb.mapper;

import org.springframework.stereotype.Component;
import ru.catwarden.advweb.dto.request.AdvertisementRequest;
import ru.catwarden.advweb.dto.response.AdvertisementResponse;
import ru.catwarden.advweb.entity.Advertisement;
import ru.catwarden.advweb.entity.User;
import ru.catwarden.advweb.enums.AdModerationStatus;

@Component
public class AdvertisementMapper {

    public AdvertisementResponse toResponse(Advertisement ad) {
        return AdvertisementResponse.builder()
                .id(ad.getId())
                .name(ad.getName())
                .description(ad.getDescription())
                .price(ad.getPrice())
                .address(ad.getAddress())
                .categoryId(ad.getCategory().getId())
                .subcategoryId(ad.getSubcategory() != null ? ad.getSubcategory().getId() : null)
                .createdAt(ad.getCreatedAt())
                .updatedAt(ad.getUpdatedAt())
                .adModerationStatus(ad.getAdModerationStatus())
                .moderationRejectionReason(ad.getModerationRejectionReason())
                .build();
    }

    public Advertisement toEntity(AdvertisementRequest advertisementRequest) {
        return Advertisement.builder()
                .name(advertisementRequest.getName())
                .description(advertisementRequest.getDescription())
                .price(advertisementRequest.getPrice())
                .address(advertisementRequest.getAddress())
                .build();
    }
}
