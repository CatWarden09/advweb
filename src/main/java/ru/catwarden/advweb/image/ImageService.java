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

    private final Path uploadDir = Paths.get("uploads");

    public List<ImageDto> uploadImage(List<MultipartFile> files) {
        List<Image> images = new ArrayList<>();

        for(MultipartFile file : files) {
            try {
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Files.createDirectories(uploadDir);
                Path filePath = uploadDir.resolve(filename);
                file.transferTo(filePath.toFile());

                Image image = new Image();
                image.setUrl("/uploads/" + filename);

                images.add(image);

            } catch (IOException e) {
                throw new RuntimeException("Не удалось сохранить файл", e);
            }
        }

        // need to get the result of DB saving to get the images generated ids
        List<Image> savedImages = imageRepository.saveAll(images);

        List<ImageDto> imageDtoList = savedImages.stream()
                .map(imageMapper::toDto)
                .toList();

        return imageDtoList;
    }
}
