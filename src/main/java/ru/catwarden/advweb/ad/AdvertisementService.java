package ru.catwarden.advweb.ad;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementUpdateRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.adcategory.AdvertisementCategory;
import ru.catwarden.advweb.entity.User;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.adcategory.CategoryRepository;
import ru.catwarden.advweb.repository.UserRepository;



@Service
@RequiredArgsConstructor
public class AdvertisementService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementMapper advertisementMapper;

    // DONE figure out mappers to avoid code repeating
    // TODO figure out MapStruct for better code
    public AdvertisementResponse getAdvertisement(Long id){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        return advertisementMapper.toResponse(advertisement);

    }

    public Page<AdvertisementResponse> getAllApprovedAdvertisements(Pageable pageable){
        return advertisementRepository.findAllByAdModerationStatus(AdModerationStatus.APPROVED, pageable)
                .map(advertisementMapper::toResponse);
    }

    public Page<AdvertisementResponse> getAllPendingAdvertisements(Pageable pageable){
        return advertisementRepository.findAllByAdModerationStatus(AdModerationStatus.PENDING, pageable)
                .map(advertisementMapper::toResponse);
    }

    public Page<AdvertisementResponse> getAllRejectedAdvertisements(Pageable pageable){
        return advertisementRepository.findAllByAdModerationStatus(AdModerationStatus.REJECTED, pageable)
                .map(advertisementMapper::toResponse);
    }

    public void createAdvertisement(AdvertisementRequest advertisementRequest){

        User author = userRepository.findById(advertisementRequest.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Author not found"));

        AdvertisementCategory category = categoryRepository.findById(advertisementRequest.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // DONE Updated the flow to keep the mapper more clean (all other fields are set after builder and checked inside the service)
        // TODO add hierarchy validation
        AdvertisementCategory subcategory = null;
        if (advertisementRequest.getSubcategoryId() != null) {
            subcategory = categoryRepository.findById(advertisementRequest.getSubcategoryId())
                    .orElseThrow(() -> new RuntimeException("Subcategory not found"));
        }

        Advertisement advertisement = advertisementMapper.toEntity(advertisementRequest);

        advertisement.setAuthor(author);
        advertisement.setCategory(category);
        advertisement.setSubcategory(subcategory);
        advertisement.setAdModerationStatus(AdModerationStatus.PENDING);

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

        advertisement.setAdModerationStatus(AdModerationStatus.PENDING);

        advertisementRepository.save(advertisement);

    }

    // TODO add status checking (cannot approve not pending)
    public void approveAdvertisement(Long id){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        advertisement.setAdModerationStatus(AdModerationStatus.APPROVED);

        advertisementRepository.save(advertisement);
    }

    public void rejectAdvertisement(Long id, String moderationRejectionReason){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        advertisement.setAdModerationStatus(AdModerationStatus.REJECTED);
        advertisement.setModerationRejectionReason(moderationRejectionReason);

        advertisementRepository.save(advertisement);
    }

    public void deleteAdvertisement(Long id){
        advertisementRepository.deleteById(id);
    }
}
