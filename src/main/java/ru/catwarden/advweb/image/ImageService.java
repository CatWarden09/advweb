package ru.catwarden.advweb.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.ad.Advertisement;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.FileOperationException;
import ru.catwarden.advweb.exception.FileStorageException;
import ru.catwarden.advweb.image.dto.ImageDto;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.storage.FileUploader;
import ru.catwarden.advweb.storage.StoredFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;

    private final FileUploader fileUploader;

    private final Path uploadDir = Paths.get("/app/uploads/adv_photos/");
    private final String fileUrl = "/uploads/adv_photos/";

    public List<ImageDto> uploadImage(List<MultipartFile> files){
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();

        List<StoredFile> uploadedFiles = fileUploader.uploadFiles(files, uploadDir);

        List<Image> images = new ArrayList<>();

        for(StoredFile uploadedImage : uploadedFiles) {
            Image image = new Image();

            image.setPath(uploadedImage.getPath());
            image.setUrl(fileUrl + uploadedImage.getFilename());
            image.setUploaderKeycloakId(currentKeycloakId);
            image.setLinkedToAd(false);

            images.add(image);
        }

        // need to get the result of DB saving to get the images generated ids
        List<Image> savedImages = imageRepository.saveAll(images);

        List<ImageDto> imageDtoList = savedImages.stream()
                .map(imageMapper::toDto)
                .toList();

        log.info("AUDIT images uploaded: userId={}, images={}", currentKeycloakId, imageDtoList.stream().map(ImageDto::getId).toList());

        return imageDtoList;
    }

    public List<String> getPreviewImageUrlByAdvertisementId(Long id){
        Image image = imageRepository.findFirstByAdId(id)
                .orElseThrow(() -> new EntityNotFoundException(Advertisement.class, id));

        List<String> images = new ArrayList<>();
        images.add(image.getUrl());

        return images;
    }
    public List<String> getImageUrlsByAdvertisementId(Long id){
        List<Image> images = imageRepository.findAllByAdId(id);

        return images.stream()
                .map(Image::getUrl)
                .toList();
    }

    // don't need to add protection here because image setting/syncing methods are called from the Advertisement service, where we already check, if the current user has access to the advertisement editing
    // DONE in FileUploader (add image format validation)
    public void setImagesToAdvertisement(List<Long> imageIds, Long advertisementId){
        List<Image> images = imageRepository.findAllById(imageIds);
        validateImagesCanBeLinked(images, imageIds, advertisementId);

        for(Image image : images){
            image.setAdId(advertisementId);
            image.setLinkedToAd(true);
        }
        imageRepository.saveAll(images);

        log.info("AUDIT images set to advertisement: advertisementId={}, images={}", advertisementId, imageIds);
    }

    public boolean syncImagesInAdvertisement(Long advertisementId, List<Long> requestImageIds){
        List<Long> currentImageIds = imageRepository.findAllByAdId(advertisementId)
                .stream()
                .map(Image::getId)
                .toList();

        Set<Long> current = new HashSet<>(currentImageIds);
        Set<Long> requested = new HashSet<>(requestImageIds);

        if (current.equals(requested)) {
            return false;
        }

        List<Long> imageIdsToUnlink = currentImageIds.stream()
                .filter(id -> !requested.contains(id))
                .toList();

        if (!imageIdsToUnlink.isEmpty()) {
            imageRepository.unlinkDeletedImagesFromAdvertisement(advertisementId, imageIdsToUnlink);
        }

        setImagesToAdvertisement(requestImageIds, advertisementId);

        return true;
    }

    private void validateImagesCanBeLinked(List<Image> images, List<Long> requestedImageIds, Long advertisementId) {
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        Set<Long> foundImageIds = images.stream()
                .map(Image::getId)
                .collect(Collectors.toSet());
        List<Long> missingIds = requestedImageIds.stream()
                .filter(id -> !foundImageIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new EntityNotFoundException(Image.class, missingIds.getFirst());
        }

        Image foreignLinkedImage = images.stream()
                .filter(image -> Boolean.TRUE.equals(image.getLinkedToAd()) && !advertisementId.equals(image.getAdId()))
                .findFirst()
                .orElse(null);
        if (foreignLinkedImage != null) {
            throw new DetailedAccessDeniedException("One or more images are linked to another advertisement",
                    Map.of(
                            "Requested advertisement id:", advertisementId,
                            "Conflicting image id:", foreignLinkedImage.getId(),
                            "Conflicting image advertisement id:", String.valueOf(foreignLinkedImage.getAdId())
                    ));
        }

        Image imageUploadedByAnotherUser = images.stream()
                .filter(image -> {
                    boolean isAlreadyInCurrentAd = advertisementId.equals(image.getAdId());
                    return !isAlreadyInCurrentAd
                            && !isAdmin
                            && !currentKeycloakId.equals(image.getUploaderKeycloakId());
                })
                .findFirst()
                .orElse(null);
        if (imageUploadedByAnotherUser != null) {
            throw new DetailedAccessDeniedException("One or more images were uploaded by another user",
                    Map.of(
                            "Requested advertisement id:", advertisementId,
                            "Conflicting image id:", imageUploadedByAnotherUser.getId(),
                            "Conflicting image uploader keycloak id:", String.valueOf(imageUploadedByAnotherUser.getUploaderKeycloakId()),
                            "Current user keycloak id:", String.valueOf(currentKeycloakId)
                    ));
        }
    }

    public void unlinkAllImagesFromAdvertisement(Long advertisementId){
        List<Image> images = imageRepository.findAllByAdId(advertisementId);

        for(Image image : images){
            image.setAdId(null);
            image.setLinkedToAd(false);
        }
        imageRepository.saveAll(images);

        log.info("AUDIT all images unlinked from advertisement: advertisementId={}", advertisementId);
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Moscow")
    public void deleteUnusedImages(){
        List<Image> images = imageRepository.findAllByLinkedToAdFalse();

        try{
            for(Image image : images){
                Files.deleteIfExists(Paths.get(image.getPath()));
            }
        } catch (IOException e){
            throw new FileStorageException("Failed to delete unused images");
        }

        imageRepository.deleteAll(images);
    }

}
