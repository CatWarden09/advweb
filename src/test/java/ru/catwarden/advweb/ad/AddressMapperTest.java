package ru.catwarden.advweb.ad;

import org.junit.jupiter.api.Test;
import ru.catwarden.advweb.ad.dto.AddressDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressMapperTest {

    private final AddressMapper mapper = new AddressMapper();

    @Test
    void toEntityMapsAllFields() {
        AddressDto dto = AddressDto.builder()
                .city("Moscow")
                .street("Tverskaya")
                .house("12A")
                .build();

        Address address = mapper.toEntity(dto);

        assertEquals("Moscow", address.getCity());
        assertEquals("Tverskaya", address.getStreet());
        assertEquals("12A", address.getHouse());
    }

    @Test
    void toDtoMapsAllFields() {
        Address address = Address.builder()
                .city("Kazan")
                .street("Baumana")
                .house("7")
                .build();

        AddressDto dto = mapper.toDto(address);

        assertEquals("Kazan", dto.getCity());
        assertEquals("Baumana", dto.getStreet());
        assertEquals("7", dto.getHouse());
    }
}

