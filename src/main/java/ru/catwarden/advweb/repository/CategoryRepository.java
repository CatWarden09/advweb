package ru.catwarden.advweb.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.catwarden.advweb.entity.AdvertisementCategory;

import java.util.List;


public interface CategoryRepository extends JpaRepository<AdvertisementCategory, Long> {

    List<AdvertisementCategory> findByParentIsNull();
    List<AdvertisementCategory> findByParent(AdvertisementCategory parent);
}
