package ru.catwarden.advweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.catwarden.advweb.entity.AdvertisementCategory;

public interface CategoryRepository extends JpaRepository<AdvertisementCategory, Long> {
}
