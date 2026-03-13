package ru.catwarden.advweb.avatar;

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
public class AvatarController {
    private final AvatarService avatarService;

    @PostMapping
    public AvatarDto uploadAvatar(@Valid @RequestParam("file") MultipartFile avatar){
        return avatarService.uploadAvatar(avatar);
    }
}
