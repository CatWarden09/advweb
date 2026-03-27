package ru.catwarden.advweb.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.catwarden.advweb.enums.Status;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByStatus(Status status, Pageable pageable);

    Page<Review> findByRecipientIdAndStatus(Long recipientId, Status status, Pageable pageable);

    Page<Review> findByAuthorIdAndStatus(Long authorId, Status status, Pageable pageable);

    @Query("""
            SELECT AVG(r.rating), COUNT(r)
            FROM Review r
            WHERE r.recipient.id = :recipientId
              AND r.status = :status
            """)
    Optional<Object[]> aggregateRatingByRecipientAndStatus(@Param("recipientId") Long recipientId,
                                                           @Param("status") Status status);
}
