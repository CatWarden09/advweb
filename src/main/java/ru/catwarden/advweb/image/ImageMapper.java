package ru.catwarden.advweb.image;

import org.springframework.stereotype.Component;
import ru.catwarden.advweb.image.dto.ImageDto;

@Component
public class ImageMapper {
    public ImageDto toDto(Image image){
        return ImageDto.builder()
                .id(image.getId())
                .url(image.getUrl())
                .build();
    }
}
