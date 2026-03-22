package ru.catwarden.advweb.adcategory;

import org.junit.jupiter.api.Test;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AdvertisementCategoryMapperTest {

    private final AdvertisementCategoryMapper mapper = new AdvertisementCategoryMapper();

    @Test
    void toResponseMapsParentIdWhenParentExists() {
        AdvertisementCategory parent = AdvertisementCategory.builder()
                .id(1L)
                .name("Transport")
                .build();
        AdvertisementCategory category = AdvertisementCategory.builder()
                .id(2L)
                .name("Cars")
                .parent(parent)
                .build();

        AdvertisementCategoryResponse response = mapper.toResponse(category);

        assertEquals(2L, response.getId());
        assertEquals("Cars", response.getName());
        assertEquals(1L, response.getParentId());
    }

    @Test
    void toResponseSetsNullParentIdWhenParentMissing() {
        AdvertisementCategory category = AdvertisementCategory.builder()
                .id(3L)
                .name("Electronics")
                .build();

        AdvertisementCategoryResponse response = mapper.toResponse(category);

        assertEquals(3L, response.getId());
        assertEquals("Electronics", response.getName());
        assertNull(response.getParentId());
    }

    @Test
    void toEntityMapsName() {
        AdvertisementCategoryRequest request = AdvertisementCategoryRequest.builder()
                .name("Furniture")
                .build();

        AdvertisementCategory category = mapper.toEntity(request);

        assertEquals("Furniture", category.getName());
    }
}

