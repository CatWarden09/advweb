package ru.catwarden.advweb.adcategory;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CategoryRepository extends JpaRepository<AdvertisementCategory, Long> {

    List<AdvertisementCategory> findByParentIsNull();
    List<AdvertisementCategory> findByParent(AdvertisementCategory parent);
}
