package ru.catwarden.advweb.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyDigestLogRepository extends JpaRepository<WeeklyDigestLog, Long> {
    boolean existsByUserIdAndWeekKey(Long userId, String weekKey);
}
