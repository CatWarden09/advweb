package ru.catwarden.advweb.image;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Images", description = "Операции с изображениями")
public class ImageController {
    private final ImageService imageService;

    @Operation(summary = "Получить изображения объявления")
    @GetMapping("/ads/{id}")
    public List<ImageDto> getImagesByAdId(@PathVariable Long id){
        return imageService.getImagesByAdId(id);
    }

    @Operation(summary = "Загрузить изображения")
    @PostMapping
    public List<ImageDto> uploadImages(@RequestParam("files") List<MultipartFile> images){
        return imageService.uploadImage(images);
    }

}
