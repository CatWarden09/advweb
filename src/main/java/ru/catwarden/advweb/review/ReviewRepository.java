package ru.catwarden.advweb.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.catwarden.advweb.enums.Status;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByStatus(Status status, Pageable pageable);

    Page<Review> findByRecipientIdAndStatus(Long recipientId, Status status, Pageable pageable);

    Page<Review> findByAuthorIdAndStatus(Long authorId, Status status, Pageable pageable);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM Review r
            WHERE r.recipient.id = :recipientId
              AND r.status = :status
            """)
    Double findAverageRatingByRecipientAndStatus(@Param("recipientId") Long recipientId,
                                                 @Param("status") Status status);

    Long countByRecipientIdAndStatus(Long recipientId, Status status);


    boolean existsByAuthorIdAndRecipientId(Long authorId, Long recipientId);

}
