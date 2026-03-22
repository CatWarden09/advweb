package ru.catwarden.advweb.ad;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.annotation.Transactional;
import ru.catwarden.advweb.adcategory.AdvertisementCategory;
import ru.catwarden.advweb.enums.AdModerationStatus;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long>, QuerydslPredicateExecutor<Advertisement> {

    @Modifying
    @Transactional
    @Query("UPDATE Advertisement a SET a.views = a.views + :count WHERE a.id = :id")
    void incrementViews(Long id, Long count);

    Page<Advertisement> findAllByAdModerationStatus(AdModerationStatus adModerationStatus, Pageable pageable);
    boolean existsByCategory(AdvertisementCategory advertisementCategory);

    Page<Advertisement> findAllByAuthorIdAndAdModerationStatus(Long authorId, AdModerationStatus adModerationStatus, Pageable pageable);

}
