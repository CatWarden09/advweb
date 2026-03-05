package ru.catwarden.advweb.image;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.image.dto.ImageDto;

import java.util.List;

// TODO add security
// DONE add unused image cleaning
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;

    @PostMapping
    public List<ImageDto> uploadImages(@RequestParam("files") List<MultipartFile> images){
        return imageService.uploadImage(images);
    }
}
