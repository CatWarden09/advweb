package ru.catwarden.advweb.ad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.catwarden.advweb.adcategory.AdvertisementCategory;
import ru.catwarden.advweb.enums.Status;
import ru.catwarden.advweb.user.User;

import java.time.LocalDateTime;

// TODO add photos,
// DONE category entity
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

    @Embedded
    private Address address;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private AdvertisementCategory category;

    @ManyToOne
    @JoinColumn(name = "subcategory_id", nullable = false)
    private AdvertisementCategory subcategory;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String moderationRejectionReason;

    // builder.default is used to guarantee that views is not null when building the object
    @Builder.Default
    @Column(nullable = false)
    private Long views = 0L;

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
