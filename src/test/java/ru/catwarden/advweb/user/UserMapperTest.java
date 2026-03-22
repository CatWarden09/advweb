package ru.catwarden.advweb.user;

import org.junit.jupiter.api.Test;
import ru.catwarden.advweb.user.dto.ShortUserInfoResponse;
import ru.catwarden.advweb.user.dto.UserResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toShortUserInfoResponseMapsFields() {
        User user = User.builder()
                .id(1L)
                .firstName("Ivan")
                .lastName("Petrov")
                .build();

        ShortUserInfoResponse result = userMapper.toShortUserInfoResponse(user);

        assertEquals(1L, result.getId());
        assertEquals("Ivan", result.getFirstName());
        assertEquals("Petrov", result.getLastName());
    }

    @Test
    void toUserResponseMapsFields() {
        User user = User.builder()
                .id(2L)
                .firstName("Petr")
                .lastName("Ivanov")
                .rating(4.8)
                .phone("+79990000000")
                .email("petr@example.com")
                .build();

        UserResponse result = userMapper.toUserResponse(user);

        assertEquals(2L, result.getId());
        assertEquals("Petr", result.getFirstName());
        assertEquals("Ivanov", result.getLastName());
        assertEquals(4.8, result.getRating());
        assertEquals("+79990000000", result.getPhone());
        assertEquals("petr@example.com", result.getEmail());
    }
}

