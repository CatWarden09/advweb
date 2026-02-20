package ru.catwarden.advweb.dto.response;
import ru.catwarden.advweb.entity.AdvertisementCategory;
import ru.catwarden.advweb.enums.AdModerationStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementResponse {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private String address;
    private AdvertisementCategory category;
    private AdvertisementCategory subcategory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AdModerationStatus adModerationStatus;
    private String moderationRejectionReason;
}
