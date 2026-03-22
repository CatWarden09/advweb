package ru.catwarden.advweb.ad;

import org.junit.jupiter.api.Test;
import ru.catwarden.advweb.ad.dto.AddressDto;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.adcategory.AdvertisementCategory;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.user.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdvertisementMapperTest {

    private final AdvertisementMapper mapper = new AdvertisementMapper(new AddressMapper());

    @Test
    void toEntityMapsMainFields() {
        AdvertisementRequest request = AdvertisementRequest.builder()
                .name("Gaming laptop")
                .description("Powerful laptop for work and games")
                .price(1200.0)
                .address(AddressDto.builder().city("Moscow").street("Lenina").house("1").build())
                .categoryId(1L)
                .subcategoryId(2L)
                .imageIds(List.of(10L, 11L))
                .build();

        Advertisement advertisement = mapper.toEntity(request);

        assertEquals("Gaming laptop", advertisement.getName());
        assertEquals("Powerful laptop for work and games", advertisement.getDescription());
        assertEquals(1200.0, advertisement.getPrice());
    }

    @Test
    void toResponseMapsAllResponseFields() {
        User author = User.builder().id(5L).build();
        AdvertisementCategory category = AdvertisementCategory.builder().id(3L).build();
        AdvertisementCategory subcategory = AdvertisementCategory.builder().id(4L).build();
        Address address = Address.builder().city("Moscow").street("Arbat").house("9").build();

        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        Advertisement advertisement = Advertisement.builder()
                .id(100L)
                .author(author)
                .name("Bike")
                .description("Good condition")
                .price(450.0)
                .address(address)
                .category(category)
                .subcategory(subcategory)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .adModerationStatus(AdModerationStatus.APPROVED)
                .moderationRejectionReason(null)
                .views(22L)
                .build();

        AdvertisementResponse response = mapper.toResponse(advertisement);

        assertEquals(100L, response.getId());
        assertEquals(5L, response.getAuthorId());
        assertEquals("Bike", response.getName());
        assertEquals("Good condition", response.getDescription());
        assertEquals(450.0, response.getPrice());
        assertEquals("Moscow", response.getAddress().getCity());
        assertEquals("Arbat", response.getAddress().getStreet());
        assertEquals("9", response.getAddress().getHouse());
        assertEquals(3L, response.getCategoryId());
        assertEquals(4L, response.getSubcategoryId());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(updatedAt, response.getUpdatedAt());
        assertEquals(AdModerationStatus.APPROVED, response.getAdModerationStatus());
        assertEquals(22L, response.getViews());
    }
}

