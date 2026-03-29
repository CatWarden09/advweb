package ru.catwarden.advweb.notification;

public record WeeklyDigestEvent(Long userId, String weekKey) {
}
