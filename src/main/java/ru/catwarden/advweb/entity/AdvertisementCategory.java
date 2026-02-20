package ru.catwarden.advweb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="advertisement_categories")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AdvertisementCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private AdvertisementCategory parent;

}
