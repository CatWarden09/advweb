package ru.catwarden.advweb.avatar;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.avatar.dto.AvatarDto;

@RestController
@RequestMapping("/avatars")
@RequiredArgsConstructor
@Validated
@Tag(name = "Аватары", description = "Операции с аватарами")
public class AvatarController {
    private final AvatarService avatarService;

    @Operation(summary = "Загрузить аватар")
    @PostMapping
    public AvatarDto uploadAvatar(
            @Parameter(description = "Файл аватара") @Valid @RequestParam("file") MultipartFile avatar){
        return avatarService.uploadAvatar(avatar);
    }
}


