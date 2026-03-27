package ru.catwarden.advweb.ad;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.catwarden.advweb.adcategory.AdvertisementCategory;
import ru.catwarden.advweb.enums.Status;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long>, QuerydslPredicateExecutor<Advertisement> {

    @Modifying
    @Transactional
    @Query("UPDATE Advertisement a SET a.views = a.views + :count WHERE a.id = :id")
    void incrementViews(Long id, Long count);

    Page<Advertisement> findAllByStatus(Status status, Pageable pageable);
    boolean existsByCategory(AdvertisementCategory advertisementCategory);

    Page<Advertisement> findAllByAuthorIdAndStatus(Long authorId, Status status, Pageable pageable);

    @Query("""
            SELECT a
            FROM User u
            JOIN u.favoriteAdvertisements a
            WHERE u.id = :userId
              AND a.status = :status
            """)
    Page<Advertisement> findFavoritesByUserIdAndStatus(@Param("userId") Long userId,
                                                       @Param("status") Status status,
                                                       Pageable pageable);

}
