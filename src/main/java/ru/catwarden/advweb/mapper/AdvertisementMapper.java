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
                .category(ad.getCategory())
                .subcategory(ad.getSubcategory())
                .createdAt(ad.getCreatedAt())
                .updatedAt(ad.getUpdatedAt())
                .adModerationStatus(ad.getAdModerationStatus())
                .moderationRejectionReason(ad.getModerationRejectionReason())
                .build();
    }

    public Advertisement toEntity(AdvertisementRequest req, User author) {
        return Advertisement.builder()
                .author(author)
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .address(req.getAddress())
                .category(req.getCategory())
                .subcategory(req.getSubcategory())
                .adModerationStatus(AdModerationStatus.PENDING)
                .build();
    }
}
