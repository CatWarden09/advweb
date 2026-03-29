package ru.catwarden.advweb.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.enums.Status;
import ru.catwarden.advweb.user.User;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyDigestScheduler {
    private final AdvertisementRepository advertisementRepository;
    private final WeeklyDigestProducer weeklyDigestProducer;

    @Scheduled(
            cron = "${app.weekly-digest.cron}",
            zone = "${app.weekly-digest.zone}"
    )
    public void scheduleWeeklyDigest() {
        List<User> users = advertisementRepository.findDistinctAuthorsByStatus(Status.APPROVED);
        String weekKey = buildWeekKey(LocalDate.now());

        for (User user : users) {
            weeklyDigestProducer.send(new WeeklyDigestEvent(user.getId(), weekKey));
        }

        log.info("AUDIT weekly digest batch scheduled: usersCount={}, weekKey={}", users.size(), weekKey);
    }

    String buildWeekKey(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        int weekBasedYear = date.get(weekFields.weekBasedYear());
        int week = date.get(weekFields.weekOfWeekBasedYear());
        return "%d-W%02d".formatted(weekBasedYear, week);
    }
}
