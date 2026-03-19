package ru.catwarden.advweb.avatar;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "avatars")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Avatar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String path;

    @Column(nullable = false, unique = true)
    private String url;

    private String uploaderKeycloakId;

    private Boolean linkedToUser;

    private Long userId;
}
