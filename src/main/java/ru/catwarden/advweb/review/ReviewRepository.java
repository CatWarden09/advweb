package ru.catwarden.advweb.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.catwarden.advweb.enums.AdModerationStatus;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByModerationStatus(AdModerationStatus moderationStatus, Pageable pageable);
}
