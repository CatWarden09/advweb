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

    // DONE figure out mappers to avoid code repeating
    // TODO figure out MapStruct for better code
    //  add moderation status validation
    public AdvertisementResponse getAdvertisement(Long id, Pageable pageable){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        Long advertisementId = advertisement.getId();

        AdvertisementResponse response = this.mapWithImages(advertisement);

        response.setComments(commentService.getAdvertisementModeratedComments(advertisementId, pageable));

        return response;
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

    // need to use @Transactional because there are multiple DB operations in single method
    // if some operations go wrong, we need to roll back all of them or then part changes will be applied to the DB
    @Transactional
    public Long createAdvertisement(AdvertisementRequest advertisementRequest){
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

        Advertisement savedAdvertisement = advertisementRepository.save(advertisement);

        imageService.setImagesToAdvertisement(
                advertisementRequest.getImageIds(),
                savedAdvertisement.getId()
        );

        return savedAdvertisement.getId();

    }

    @Transactional
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

        // TODO add no updates check
        List<Long> imageIds = advertisementUpdateRequest.getImageIds();
        if(imageIds != null) {
            imageService.unlinkImagesFromAdvertisement(id, imageIds);
            imageService.setImagesToAdvertisement(imageIds, id);
        } else{
            throw new RuntimeException("Advertisement must have at least one image");
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

    // TODO add cascade images deletion (or assigned to ad = false and auto deletion later)
    public void deleteAdvertisement(Long id){
        advertisementRepository.deleteById(id);

        commentService.deleteCommentsByAdId(id);
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

