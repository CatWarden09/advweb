package ru.catwarden.advweb.avatar;

import org.junit.jupiter.api.Test;
import ru.catwarden.advweb.avatar.dto.AvatarDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AvatarMapperTest {

    private final AvatarMapper avatarMapper = new AvatarMapper();

    @Test
    void toDtoMapsIdAndUrl() {
        Avatar avatar = Avatar.builder()
                .id(10L)
                .url("/uploads/user_avatars/a.png")
                .build();

        AvatarDto result = avatarMapper.toDto(avatar);

        assertEquals(10L, result.getId());
        assertEquals("/uploads/user_avatars/a.png", result.getUrl());
    }
}

