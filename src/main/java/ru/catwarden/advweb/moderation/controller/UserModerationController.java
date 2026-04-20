package ru.catwarden.advweb.moderation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.catwarden.advweb.user.UserService;

@RestController
@RequestMapping("/admin/user-moderation")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users Moderation", description = "Модерация пользователей")
public class UserModerationController {
    private final UserService userService;

    @Operation(summary = "Удалить аватар пользователя (модерация)")
    @DeleteMapping("/{id}/avatar")
    public void deleteUserAvatar(@PathVariable Long id) {
        userService.unlinkUserAvatar(id);
    }
}
