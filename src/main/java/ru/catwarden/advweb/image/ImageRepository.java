package ru.catwarden.advweb.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findAllByLinkedToAdFalse();

    List<Image> findAllByAdId(Long adId);

    Optional<Image> findFirstByAdId(Long adId);

    void unlinkDeletedImagesFromAdvertisement(Long adId, List<Long> imageIds);


}
