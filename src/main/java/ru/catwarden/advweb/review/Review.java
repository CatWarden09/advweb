package ru.catwarden.advweb.review;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.catwarden.advweb.enums.AdModerationStatus;
import ru.catwarden.advweb.user.User;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false)
    private AdModerationStatus moderationStatus;

    private String moderationRejectionReason;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }


}