package ru.catwarden.advweb.ad;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.catwarden.advweb.adcategory.AdvertisementCategory;
import ru.catwarden.advweb.enums.AdModerationStatus;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    Page<Advertisement> findAllByAdModerationStatus(AdModerationStatus adModerationStatus, Pageable pageable);
    boolean existsByCategory(AdvertisementCategory advertisementCategory);

    Page<Advertisement> findAllByAuthorIdAndAdModerationStatus(Long authorId, AdModerationStatus adModerationStatus, Pageable pageable);

}
