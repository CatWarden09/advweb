package ru.catwarden.advweb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.dto.request.AdvertismentRequest;
import ru.catwarden.advweb.entity.Advertisement;
import ru.catwarden.advweb.entity.User;
import ru.catwarden.advweb.repository.AdvertisementRepository;
import ru.catwarden.advweb.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdvertisementService {
    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;

    public void createAdvertisement(AdvertismentRequest advertismentRequest){

        User author = userRepository.findById(advertismentRequest.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Author not found"));

        Advertisement advertisement = Advertisement.builder()
                .author(author)
                .name(advertismentRequest.getName())
                .description(advertismentRequest.getDescription())
                .price(advertismentRequest.getPrice())
                .address(advertismentRequest.getAddress())
                .category(advertismentRequest.getCategory())
                .subcategory(advertismentRequest.getSubcategory())
                .build();

        advertisementRepository.save(advertisement);

    }
}
