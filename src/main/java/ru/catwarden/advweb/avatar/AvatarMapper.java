package ru.catwarden.advweb.avatar;

import org.springframework.stereotype.Component;
import ru.catwarden.advweb.avatar.dto.AvatarDto;

@Component
public class AvatarMapper {
    public AvatarDto toDto(Avatar avatar){
        return AvatarDto.builder()
                .id(avatar.getId())
                .url(avatar.getUrl())
                .build();
    }
}
