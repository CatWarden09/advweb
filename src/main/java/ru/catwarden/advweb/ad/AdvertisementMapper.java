package ru.catwarden.advweb.ad;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;

@Component
@RequiredArgsConstructor
public class AdvertisementMapper {
    private final AddressMapper addressMapper;

    public AdvertisementResponse toResponse(Advertisement ad) {
        return AdvertisementResponse.builder()
                .id(ad.getId())
                .authorId(ad.getAuthor().getId())
                .name(ad.getName())
                .description(ad.getDescription())
                .price(ad.getPrice())
                .address(addressMapper.toDto(ad.getAddress()))
                .categoryId(ad.getCategory().getId())
                .subcategoryId(ad.getSubcategory().getId())
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
                .build();
    }
}
