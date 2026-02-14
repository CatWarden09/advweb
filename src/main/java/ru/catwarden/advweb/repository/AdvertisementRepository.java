package ru.catwarden.advweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.catwarden.advweb.entity.Advertisement;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

}
