package ru.catwarden.advweb.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.image.dto.ImageDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;

    private final Path uploadDir = Paths.get("/app/uploads");

    public List<ImageDto> uploadImage(List<MultipartFile> files){
        List<Image> images = new ArrayList<>();

        try{
            Files.createDirectories(uploadDir);
        } catch (IOException e){
            throw new RuntimeException("Failed to create directory");
        }

        for(MultipartFile file : files) {
            try {
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path filePath = uploadDir.resolve(filename);

                file.transferTo(filePath.toFile());

                Image image = new Image();
                image.setUrl("/uploads/" + filename);

                images.add(image);

            } catch (IOException e) {
                throw new RuntimeException("Failed to save the file", e);
            }
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

    public void unlinkImagesFromAdvertisement(Long advertisementId, List<Long> imageIds){
        imageRepository.unlinkImagesFromAdvertisement(advertisementId, imageIds);
    }

    public List<Image> findUnusedImages(){
        return imageRepository.findAllByLinkedToAdFalse();
    }

}
