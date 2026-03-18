package ru.catwarden.advweb.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.catwarden.advweb.enums.AdModerationStatus;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByModerationStatus(AdModerationStatus moderationStatus, Pageable pageable);

    Page<Review> findByRecipientIdAndModerationStatus(Long recipientId, AdModerationStatus moderationStatus, Pageable pageable);

    Page<Review> findByAuthorIdAndModerationStatus(Long authorId, AdModerationStatus moderationStatus, Pageable pageable);

    @Query("""
            SELECT AVG(r.rating), COUNT(r)
            FROM Review r
            WHERE r.recipient.id = :recipientId
              AND r.moderationStatus = :moderationStatus
            """)
    Optional<Object[]> aggregateRatingByRecipientAndStatus(@Param("recipientId") Long recipientId,
                                                           @Param("moderationStatus") AdModerationStatus moderationStatus);
}
