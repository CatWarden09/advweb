package ru.catwarden.advweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.catwarden.advweb.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
