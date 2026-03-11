package ru.catwarden.advweb.ad;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementUpdateRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.adcategory.AdvertisementCategory;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.adcategory.CategoryRepository;
import ru.catwarden.advweb.image.ImageService;
import ru.catwarden.advweb.user.UserRepository;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AdvertisementService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementMapper advertisementMapper;

    private final ImageService imageService;
    private final CommentService commentService;

    private final int MAX_IMAGES_PER_AD = 10;

    // DONE figure out mappers to avoid code repeating
    // TODO figure out MapStruct for better code
    // DONE moderation status validation
    public AdvertisementResponse getAdvertisement(Long id){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        return this.mapWithImages(advertisement);
    }

    public Page<AdvertisementResponse> getAllApprovedAdvertisements(Pageable pageable){
        return advertisementRepository.findAllByAdModerationStatus(AdModerationStatus.APPROVED, pageable)
                .map(this::mapWithPreviewImage);
    }

    public Page<AdvertisementResponse> getAllPendingAdvertisements(Pageable pageable){
        return advertisementRepository.findAllByAdModerationStatus(AdModerationStatus.PENDING, pageable)
                .map(this::mapWithPreviewImage);
    }

    public Page<AdvertisementResponse> getAllRejectedAdvertisements(Pageable pageable){
        return advertisementRepository.findAllByAdModerationStatus(AdModerationStatus.REJECTED, pageable)
                .map(this::mapWithPreviewImage);
    }

    public Page<AdvertisementResponse> getUserApprovedAdvertisements(Long userId, Pageable pageable){
        return advertisementRepository.findAllByAuthorIdAndAdModerationStatus(userId, AdModerationStatus.APPROVED, pageable)
                .map(this::mapWithPreviewImage);
    }

    public Page<AdvertisementResponse> getUserPendingAdvertisements(Long userId, Pageable pageable){
        return advertisementRepository.findAllByAuthorIdAndAdModerationStatus(userId, AdModerationStatus.PENDING, pageable)
                .map(this::mapWithPreviewImage);
    }

    public Page<AdvertisementResponse> getUserRejectedAdvertisements(Long userId, Pageable pageable){
        return advertisementRepository.findAllByAuthorIdAndAdModerationStatus(userId, AdModerationStatus.REJECTED, pageable)
                .map(this::mapWithPreviewImage);
    }

    // need to use @Transactional because there are multiple DB operations in single method
    // if some operations go wrong, we need to roll back all of them or then part changes will be applied to the DB
    @Transactional
    public Long createAdvertisement(AdvertisementRequest advertisementRequest){
        User author = userRepository.findById(advertisementRequest.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Author not found"));

        AdvertisementCategory category = categoryRepository.findById(advertisementRequest.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // DONE Updated the flow to keep the mapper more clean (all other fields are set after builder and checked inside the service)
        // DONE add hierarchy validation
        AdvertisementCategory subcategory = null;
        if (advertisementRequest.getSubcategoryId() != null) {
            subcategory = categoryRepository.findById(advertisementRequest.getSubcategoryId())
                    .orElseThrow(() -> new RuntimeException("Subcategory not found"));
        }

        if (subcategory != null && !subcategory.getParent().equals(category)) {
            throw new RuntimeException("Subcategory is not a child of the given category");
        }

        if (advertisementRequest.getImageIds().size() > MAX_IMAGES_PER_AD) {
            throw new RuntimeException("Limit for advertisement pictures is exceeded");
        }

        Advertisement advertisement = advertisementMapper.toEntity(advertisementRequest);

        advertisement.setAuthor(author);
        advertisement.setCategory(category);
        advertisement.setSubcategory(subcategory);
        advertisement.setAdModerationStatus(AdModerationStatus.PENDING);

        Advertisement savedAdvertisement = advertisementRepository.save(advertisement);

        imageService.setImagesToAdvertisement(
                advertisementRequest.getImageIds(),
                savedAdvertisement.getId()
        );

        return savedAdvertisement.getId();

    }

    @Transactional
    public void updateAdvertisement(Long id, AdvertisementUpdateRequest advertisementUpdateRequest){
        boolean isFieldsChanged = false;
        boolean isImagesChanged = false;

        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        if (advertisementUpdateRequest.getImageIds().size() > MAX_IMAGES_PER_AD) {
            throw new RuntimeException("Limit for advertisement pictures is exceeded");
        }

        if(!advertisement.getName().equals(advertisementUpdateRequest.getName())
                || !advertisement.getDescription().equals(advertisementUpdateRequest.getDescription())
                || !advertisement.getPrice().equals(advertisementUpdateRequest.getPrice())
                || !advertisement.getAddress().equals(advertisementUpdateRequest.getAddress())){

            isFieldsChanged = true;
        }


        // DONE add no updates check
        List<Long> requestImageIds = advertisementUpdateRequest.getImageIds();

        isImagesChanged = imageService.syncImagesInAdvertisement(id, requestImageIds);

        if(!isFieldsChanged && !isImagesChanged){
            return;
        }

        advertisement.setName(advertisementUpdateRequest.getName());
        advertisement.setDescription(advertisementUpdateRequest.getDescription());
        advertisement.setPrice(advertisementUpdateRequest.getPrice());
        advertisement.setAddress(advertisementUpdateRequest.getAddress());

        advertisement.setAdModerationStatus(AdModerationStatus.PENDING);

    }

    // DONE add status checking (cannot approve/reject not pending)
    public void approveAdvertisement(Long id){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        if(advertisement.getAdModerationStatus() != AdModerationStatus.PENDING){
            throw new RuntimeException("Cannot change status of a non-pending advertisement");
        }

        advertisement.setAdModerationStatus(AdModerationStatus.APPROVED);

        advertisementRepository.save(advertisement);
    }

    public void rejectAdvertisement(Long id, String moderationRejectionReason){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        if(advertisement.getAdModerationStatus() != AdModerationStatus.PENDING){
            throw new RuntimeException("Cannot change status of a non-pending advertisement");
        }

        advertisement.setAdModerationStatus(AdModerationStatus.REJECTED);
        advertisement.setModerationRejectionReason(moderationRejectionReason);

        advertisementRepository.save(advertisement);
    }

    public void deleteAdvertisement(Long id){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        advertisementRepository.deleteById(advertisement.getId());

        commentService.deleteCommentsByAdId(advertisement.getId());

        imageService.unlinkAllImagesFromAdvertisement(advertisement.getId());
    }

    // use private mapper to avoid code repeating in get ads methods and not to overload Ad entity with all the images
    // (we get images only when we open the add or show the ad list (and then we use the preview image and not all of them))
    // 1st method is for ads list (get only 1st image)
    // 2nd method is for specific ad page (get all images)
    private AdvertisementResponse mapWithPreviewImage(Advertisement advertisement){
        AdvertisementResponse advertisementResponse = advertisementMapper.toResponse(advertisement);
        advertisementResponse.setImageUrls(imageService.getPreviewImageUrlByAdvertisementId(advertisement.getId()));

        return advertisementResponse;
    }
    private AdvertisementResponse mapWithImages(Advertisement advertisement){
        AdvertisementResponse advertisementResponse = advertisementMapper.toResponse(advertisement);
        advertisementResponse.setImageUrls(imageService.getImageUrlsByAdvertisementId(advertisement.getId()));

        return advertisementResponse;
    }
}

