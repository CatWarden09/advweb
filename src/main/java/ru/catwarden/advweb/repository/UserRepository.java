package ru.catwarden.advweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.catwarden.advweb.user.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
