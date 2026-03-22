package ru.catwarden.advweb.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.image.dto.ImageDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

    @Mock
    private ImageService imageService;

    @Mock
    private MultipartFile multipartFile1;
    @Mock
    private MultipartFile multipartFile2;

    private ImageController imageController;

    @BeforeEach
    void setUp() {
        imageController = new ImageController(imageService);
    }

    @Test
    void uploadImagesDelegatesToService() {
        List<MultipartFile> files = List.of(multipartFile1, multipartFile2);
        List<ImageDto> response = List.of(
                ImageDto.builder().id(1L).url("/uploads/adv_photos/1.png").build(),
                ImageDto.builder().id(2L).url("/uploads/adv_photos/2.png").build()
        );
        when(imageService.uploadImage(files)).thenReturn(response);

        List<ImageDto> result = imageController.uploadImages(files);

        assertEquals(response, result);
    }
}

