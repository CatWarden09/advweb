package ru.catwarden.advweb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.dto.request.AdvertisementRequest;
import ru.catwarden.advweb.dto.request.AdvertisementUpdateRequest;
import ru.catwarden.advweb.dto.response.AdvertisementResponse;
import ru.catwarden.advweb.entity.Advertisement;
import ru.catwarden.advweb.entity.User;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.repository.AdvertisementRepository;
import ru.catwarden.advweb.repository.UserRepository;


@Service
@RequiredArgsConstructor
public class AdvertisementService {
    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;

    // TODO figure out mappers to avoid code repeating
    public AdvertisementResponse getAdvertisement(Long id){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        AdvertisementResponse advertisementResponse = AdvertisementResponse.builder()
                .id(advertisement.getId())
                .name(advertisement.getName())
                .description(advertisement.getDescription())
                .price(advertisement.getPrice())
                .address(advertisement.getAddress())
                .category(advertisement.getCategory())
                .subcategory(advertisement.getSubcategory())
                .createdAt(advertisement.getCreatedAt())
                .updatedAt(advertisement.getUpdatedAt())
                .adModerationStatus(advertisement.getAdModerationStatus())
                .moderationRejectionReason(advertisement.getModerationRejectionReason())
                .build();

        return advertisementResponse;
    }

    public void createAdvertisement(AdvertisementRequest advertisementRequest){

        User author = userRepository.findById(advertisementRequest.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Author not found"));

        Advertisement advertisement = Advertisement.builder()
                .author(author)
                .name(advertisementRequest.getName())
                .description(advertisementRequest.getDescription())
                .price(advertisementRequest.getPrice())
                .address(advertisementRequest.getAddress())
                .category(advertisementRequest.getCategory())
                .subcategory(advertisementRequest.getSubcategory())
                .adModerationStatus(AdModerationStatus.PENDING)
                .build();

        advertisementRepository.save(advertisement);

    }

    public void updateAdvertisement(Long id, AdvertisementUpdateRequest advertisementUpdateRequest){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        if(advertisementUpdateRequest.getName() != null) {
            advertisement.setName(advertisementUpdateRequest.getName());
        }
        if(advertisementUpdateRequest.getDescription() != null) {
            advertisement.setDescription(advertisementUpdateRequest.getDescription());
        }
        if(advertisementUpdateRequest.getPrice() != null) {
            advertisement.setPrice(advertisementUpdateRequest.getPrice());
        }
        if(advertisementUpdateRequest.getAddress() != null) {
            advertisement.setAddress(advertisementUpdateRequest.getAddress());
        }

        advertisementRepository.save(advertisement);

    }

    public void deleteAdvertisement(Long id){
        advertisementRepository.deleteById(id);
    }
}
