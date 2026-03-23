package ru.catwarden.advweb.ad;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.ad.dto.AdvertisementSearchFilter;
import ru.catwarden.advweb.ad.dto.AdvertisementUpdateRequest;
import ru.catwarden.advweb.adcategory.AdvertisementCategory;
import ru.catwarden.advweb.adcategory.CategoryRepository;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.InvalidRelationException;
import ru.catwarden.advweb.exception.InvalidStateException;
import ru.catwarden.advweb.exception.LimitExceededException;
import ru.catwarden.advweb.image.ImageService;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserRepository;

import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class AdvertisementService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AdvertisementRepository advertisementRepository;

    private final AdvertisementMapper advertisementMapper;
    private final AddressMapper addressMapper;

    private final ImageService imageService;
    private final CommentService commentService;
    private final ViewCountService viewCountService;

    private final int MAX_IMAGES_PER_AD = 10;

    // DONE figure out mappers to avoid code repeating
    // DONE moderation status validation
    // TODO add entitynotfoundexception
    public AdvertisementResponse getAdvertisement(Long id){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Advertisement.class, id));
        viewCountService.increment(id);

        return this.mapWithPreviewImage(advertisement);
    }

    public Page<AdvertisementResponse> getAdvertisementsByFilter(Pageable pageable, AdvertisementSearchFilter filter){
        QAdvertisement advertisement = QAdvertisement.advertisement;

        BooleanBuilder builder = new BooleanBuilder();

        if(filter.getName() != null){
            builder.and(advertisement.name.containsIgnoreCase(filter.getName()));
        }
        if(filter.getDescription() != null){
            builder.and(advertisement.description.containsIgnoreCase(filter.getDescription()));
        }
        if(filter.getPriceMin() != null)
            builder.and(advertisement.price.goe(filter.getPriceMin()));

        if(filter.getPriceMax() != null)
            builder.and(advertisement.price.loe(filter.getPriceMax()));

        if(filter.getCategoryId() != null){
            builder.and(advertisement.category.id.eq(filter.getCategoryId()));
        }
        if(filter.getSubcategoryId() != null){
            builder.and(advertisement.subcategory.id.eq(filter.getSubcategoryId()));
        }
        builder.and(advertisement.adModerationStatus.eq(AdModerationStatus.APPROVED));

        return advertisementRepository.findAll(builder, pageable)
                .map(this::mapWithImages);
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
        validateCurrentUserOrAdmin(userId);
        return advertisementRepository.findAllByAuthorIdAndAdModerationStatus(userId, AdModerationStatus.PENDING, pageable)
                .map(this::mapWithPreviewImage);
    }

    public Page<AdvertisementResponse> getUserRejectedAdvertisements(Long userId, Pageable pageable){
        validateCurrentUserOrAdmin(userId);
        return advertisementRepository.findAllByAuthorIdAndAdModerationStatus(userId, AdModerationStatus.REJECTED, pageable)
                .map(this::mapWithPreviewImage);
    }

    // need to use @Transactional because there are multiple DB operations in single method
    // if some operations go wrong, we need to roll back all of them or then part changes will be applied to the DB
    @Transactional
    public Long createAdvertisement(AdvertisementRequest advertisementRequest){
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        User author = userRepository.findByKeycloakId(currentKeycloakId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, currentKeycloakId));

        AdvertisementCategory category = categoryRepository.findById(advertisementRequest.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, advertisementRequest.getCategoryId()));

        // DONE Updated the flow to keep the mapper more clean (all other fields are set after builder and checked inside the service)
        // DONE add hierarchy validation
        AdvertisementCategory subcategory = categoryRepository.findById(advertisementRequest.getSubcategoryId())
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, advertisementRequest.getSubcategoryId()));

        if (!subcategory.getParent().equals(category)) {
            throw new InvalidRelationException("Subcategory is not a child of the given category",
                    Map.of("Subcategory id:", subcategory.getId(), "Parent category id:", category.getId()));
        }

        if (subcategory.equals(category)){
            throw new InvalidRelationException("Subcategory cannot be the same as the category",
                    Map.of("Subcategory id:", subcategory.getId(), "Parent category id:", category.getId()));
        }

        if (advertisementRequest.getImageIds().size() > MAX_IMAGES_PER_AD) {
            throw new LimitExceededException("Limit for advertisement pictures is exceeded");
        }



        Advertisement advertisement = advertisementMapper.toEntity(advertisementRequest);

        advertisement.setAuthor(author);
        advertisement.setCategory(category);
        advertisement.setSubcategory(subcategory);

        Address address = addressMapper.toEntity(advertisementRequest.getAddress());
        advertisement.setAddress(address);

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
                .orElseThrow(() -> new EntityNotFoundException(Advertisement.class, id));

        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && !advertisement.getAuthor().getKeycloakId().equals(currentKeycloakId)) {
            throw new AccessDeniedException("You are not allowed to update this advertisement");
        }

        boolean isFieldsChanged = false;
        boolean isImagesChanged;

        if (advertisementUpdateRequest.getImageIds().size() > MAX_IMAGES_PER_AD) {
            throw new LimitExceededException("Limit for advertisement pictures is exceeded");
        }

        Address requestAddress = addressMapper.toEntity(advertisementUpdateRequest.getAddress());

        if(!advertisement.getName().equals(advertisementUpdateRequest.getName())
                || !advertisement.getDescription().equals(advertisementUpdateRequest.getDescription())
                || !advertisement.getPrice().equals(advertisementUpdateRequest.getPrice())
                || !advertisement.getAddress().equals(requestAddress)){

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
        advertisement.setAddress(requestAddress);

        advertisement.setAdModerationStatus(AdModerationStatus.PENDING);

    }

    // DONE add status checking (cannot approve/reject not pending)
    public void approveAdvertisement(Long id){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Advertisement.class, id));

        if(advertisement.getAdModerationStatus() != AdModerationStatus.PENDING){
            throw new InvalidStateException("Cannot change status of a non-pending advertisement");
        }

        advertisement.setAdModerationStatus(AdModerationStatus.APPROVED);

        advertisementRepository.save(advertisement);
    }

    public void rejectAdvertisement(Long id, String moderationRejectionReason){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Advertisement.class, id));

        if(advertisement.getAdModerationStatus() != AdModerationStatus.PENDING){
            throw new InvalidStateException("Cannot change status of a non-pending advertisement");
        }

        advertisement.setAdModerationStatus(AdModerationStatus.REJECTED);
        advertisement.setModerationRejectionReason(moderationRejectionReason);

        advertisementRepository.save(advertisement);
    }

    public void deleteAdvertisement(Long id){
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Advertisement.class, id));

        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && !advertisement.getAuthor().getKeycloakId().equals(currentKeycloakId)) {
            throw new AccessDeniedException("You are not allowed to delete this advertisement");
        }

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

    private void validateCurrentUserOrAdmin(Long userId) {
        if (SecurityUtils.isCurrentUserAdmin()) {
            return;
        }

        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        User requestedUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, userId));

        if (!currentKeycloakId.equals(requestedUser.getKeycloakId())) {
            throw new AccessDeniedException("You can only view your own advertisements");
        }
    }
}

