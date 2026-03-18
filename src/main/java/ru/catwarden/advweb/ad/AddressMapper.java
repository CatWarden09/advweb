package ru.catwarden.advweb.ad;

import org.springframework.stereotype.Component;
import ru.catwarden.advweb.ad.dto.AddressDto;

@Component
public class AddressMapper {
    public Address toEntity(AddressDto dto) {
        return Address.builder()
                .city(dto.getCity())
                .street(dto.getStreet())
                .house(dto.getHouse())
                .build();
    }

    public AddressDto toDto(Address entity) {
        return AddressDto.builder()
                .city(entity.getCity())
                .street(entity.getStreet())
                .house(entity.getHouse())
                .build();
    }
}
