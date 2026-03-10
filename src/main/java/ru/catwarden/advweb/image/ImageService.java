package ru.catwarden.advweb.image;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.image.dto.ImageDto;
import ru.catwarden.advweb.storage.FileUploader;
import ru.catwarden.advweb.storage.StoredFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;

    private final FileUploader fileUploader;

    private final Path uploadDir = Paths.get("/app/uploads/adv_photos/");
    private final String fileUrl = "/uploads/adv_photos/";

    public List<ImageDto> uploadImage(List<MultipartFile> files){
        List<StoredFile> uploadedFiles = fileUploader.uploadFiles(files, uploadDir);

        List<Image> images = new ArrayList<>();

        for(StoredFile uploadedImage : uploadedFiles) {
            Image image = new Image();

            image.setPath(uploadedImage.getPath());
            image.setUrl(fileUrl + uploadedImage.getFilename());
            image.setLinkedToAd(false);

            images.add(image);
        }

        // need to get the result of DB saving to get the images generated ids
        List<Image> savedImages = imageRepository.saveAll(images);

        List<ImageDto> imageDtoList = savedImages.stream()
                .map(imageMapper::toDto)
                .toList();

        return imageDtoList;
    }

    public List<String> getPreviewImageUrlByAdvertisementId(Long id){
        Image image = imageRepository.findFirstByAdId(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

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

    // TODO add image format validation
    public void setImagesToAdvertisement(List<Long> imageIds, Long advertisementId){
        List<Image> images = imageRepository.findAllById(imageIds);

        for(Image image : images){
            image.setAdId(advertisementId);
            image.setLinkedToAd(true);
        }
        imageRepository.saveAll(images);
    }

    // NOTE found out another way to do this with .filter(), without custom SQL (more flexible, less resource-efficient)
    //    List<Image> images = imageRepository.findAllByAdId(advertisementId);
    //    List<Image> toUnlink = images.stream()
    //            .filter(image -> !imageIds.contains(image.getId()))
    //            .toList();
    //    imageRepository.saveAll(toUnlink);
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

    public void unlinkAllImagesFromAdvertisement(Long advertisementId){
        List<Image> images = imageRepository.findAllByAdId(advertisementId);

        for(Image image : images){
            image.setAdId(null);
            image.setLinkedToAd(false);
        }
        imageRepository.saveAll(images);
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Moscow")
    public void deleteUnusedImages(){
        List<Image> images = imageRepository.findAllByLinkedToAdFalse();

        try{
            for(Image image : images){
                Files.deleteIfExists(Paths.get(image.getPath()));
            }
        } catch (IOException e){
            throw new RuntimeException("Failed to delete unused images");
        }

        imageRepository.deleteAll(images);
    }

}
