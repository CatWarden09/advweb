package ru.catwarden.advweb.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.catwarden.advweb.avatar.AvatarService;
import ru.catwarden.advweb.user.dto.ShortUserInfoResponse;
import ru.catwarden.advweb.user.dto.UserResponse;


// this class is introduced because we need to set the avatar url in the response. To avoid code repetition and keep the mapper clean
// this assembler helps with the task
@Component
@RequiredArgsConstructor
public class UserResponseAssembler {
    private final UserMapper userMapper;
    private final AvatarService avatarService;

    public UserResponse toUserResponse(User user) {
        UserResponse response = userMapper.toUserResponse(user);
        response.setAvatarUrl(avatarService.findUserAvatarUrl(user.getId()).orElse(null));
        return response;
    }

    public ShortUserInfoResponse toShortUserInfoResponse(User user) {
        ShortUserInfoResponse response = userMapper.toShortUserInfoResponse(user);
        response.setAvatarUrl(avatarService.findUserAvatarUrl(user.getId()).orElse(null));
        return response;
    }
}
