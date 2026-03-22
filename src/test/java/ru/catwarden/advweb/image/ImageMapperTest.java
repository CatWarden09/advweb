package ru.catwarden.advweb.image;

import org.junit.jupiter.api.Test;
import ru.catwarden.advweb.image.dto.ImageDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageMapperTest {

    private final ImageMapper imageMapper = new ImageMapper();

    @Test
    void toDtoMapsIdAndUrl() {
        Image image = Image.builder()
                .id(5L)
                .url("/uploads/adv_photos/abc.png")
                .build();

        ImageDto result = imageMapper.toDto(image);

        assertEquals(5L, result.getId());
        assertEquals("/uploads/adv_photos/abc.png", result.getUrl());
    }
}

