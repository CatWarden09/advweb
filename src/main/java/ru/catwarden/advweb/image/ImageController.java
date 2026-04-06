package ru.catwarden.advweb.image;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/ads/{id}")
    public List<ImageDto> getImagesByAdId(@PathVariable Long id){
        return imageService.getImagesByAdId(id);
    }

    @PostMapping
    public List<ImageDto> uploadImages(@RequestParam("files") List<MultipartFile> images){
        return imageService.uploadImage(images);
    }

}
