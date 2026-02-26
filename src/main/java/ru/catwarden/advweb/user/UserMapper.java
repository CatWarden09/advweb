package ru.catwarden.advweb.user;

import org.springframework.stereotype.Component;
import ru.catwarden.advweb.user.dto.ShortUserInfoResponse;

@Component
public class UserMapper {
    public ShortUserInfoResponse toShortUserInfoResponse(User user) {
        return ShortUserInfoResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .rating(user.getRating())
                .phone(user.getPhone())
                .build();
    }
}
