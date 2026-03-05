package ru.catwarden.advweb.avatar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {
    Optional<Avatar> findByUserId(Long userId);

    List<Avatar> findAllByLinkedToUserFalse();
}
