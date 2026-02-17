package ru.catwarden.advweb.dto.request;


import ru.catwarden.advweb.entity.User;
import ru.catwarden.advweb.enums.AdModerationStatus;

import java.time.LocalDateTime;

public class AdvertismentRequest {

    private Long authorId;
    private String name;
    private String description;
    private Double price;
    private String address;
    private String category;
    private String subcategory;

}
