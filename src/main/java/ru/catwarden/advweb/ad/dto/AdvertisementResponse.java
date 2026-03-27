package ru.catwarden.advweb.ad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.catwarden.advweb.enums.Status;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementResponse {
    private Long id;
    private Long authorId;
    private String name;
    private String description;
    private Double price;
    private AddressDto address;
    private Long categoryId;
    private Long subcategoryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Status status;
    private String moderationRejectionReason;
    private List<String> imageUrls;
    private Long views;
}
