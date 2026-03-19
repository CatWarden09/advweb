package ru.catwarden.advweb.moderation.controller;

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
public class UserModerationController {
    private final UserService userService;

    @DeleteMapping("/{id}/avatar")
    public void deleteUserAvatar(@PathVariable Long id) {
        userService.unlinkUserAvatar(id);
    }
}
