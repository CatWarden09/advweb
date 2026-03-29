package ru.catwarden.advweb.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "weekly_digest_log",
        uniqueConstraints = @UniqueConstraint(name = "uc_weekly_digest_log_user_week", columnNames = {"user_id", "week_key"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyDigestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_key", nullable = false)
    private String weekKey;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
