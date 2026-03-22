package ru.catwarden.advweb.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.catwarden.advweb.avatar.AvatarService;
import ru.catwarden.advweb.user.dto.ShortUserInfoResponse;
import ru.catwarden.advweb.user.dto.UserResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserResponseAssemblerTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private AvatarService avatarService;

    @InjectMocks
    private UserResponseAssembler userResponseAssembler;

    @Test
    void toUserResponseAddsAvatarUrl() {
        User user = User.builder().id(5L).build();
        UserResponse mapped = UserResponse.builder().id(5L).firstName("Ivan").build();
        when(userMapper.toUserResponse(user)).thenReturn(mapped);
        when(avatarService.findUserAvatarUrl(5L)).thenReturn(Optional.of("/uploads/user_avatars/5.png"));

        UserResponse result = userResponseAssembler.toUserResponse(user);

        assertEquals("/uploads/user_avatars/5.png", result.getAvatarUrl());
    }

    @Test
    void toShortUserInfoResponseAddsAvatarUrl() {
        User user = User.builder().id(5L).build();
        ShortUserInfoResponse mapped = ShortUserInfoResponse.builder().id(5L).firstName("Ivan").build();
        when(userMapper.toShortUserInfoResponse(user)).thenReturn(mapped);
        when(avatarService.findUserAvatarUrl(5L)).thenReturn(Optional.of("/uploads/user_avatars/5.png"));

        ShortUserInfoResponse result = userResponseAssembler.toShortUserInfoResponse(user);

        assertEquals("/uploads/user_avatars/5.png", result.getAvatarUrl());
    }
}

