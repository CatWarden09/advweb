package ru.catwarden.advweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.catwarden.advweb.entity.Advertisement;
import ru.catwarden.advweb.entity.AdvertisementCategory;

import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    boolean existsByCategory(AdvertisementCategory advertisementCategory);

}
