package ru.catwarden.advweb.dto.request;

import ru.catwarden.advweb.enums.AdModerationStatus;

import java.time.LocalDateTime;

public class AdvertisementResponse {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private String address;
    private String category;
    private String subcategory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AdModerationStatus adModerationStatus;
    private String moderationRejectionReason;
}
