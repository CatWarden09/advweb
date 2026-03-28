package ru.catwarden.advweb.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.catwarden.advweb.ad.Advertisement;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    private Double rating;

    private Long ratingCount;

    private Long avatarId;

    @Column(unique = true)
    private String keycloakId;

    @Column
    private String password;

    @ManyToMany
    @JoinTable(
            name = "user_favorite_advertisements",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "advertisement_id")
    )
    @Builder.Default
    private List<Advertisement> favoriteAdvertisements = new ArrayList<>();

    private Double totalEarned;

}
