package ru.catwarden.advweb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.catwarden.advweb.enums.AdModerationStatus;

import java.time.LocalDateTime;

// TODO add photos, category entity(?)
@Entity
@Table(name = "advertisements")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double price;

    private String address;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private AdvertisementCategory category;

    @ManyToOne
    @JoinColumn(name = "subcategory_id", nullable = false)
    private AdvertisementCategory subcategory;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private AdModerationStatus adModerationStatus;

    private String moderationRejectionReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}
