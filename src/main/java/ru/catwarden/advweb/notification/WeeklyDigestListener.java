package ru.catwarden.advweb.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyDigestListener {
    private final WeeklyDigestService weeklyDigestService;

    @KafkaListener(
            topics = "${app.kafka.topics.weekly-digest}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleWeeklyDigest(WeeklyDigestEvent event) {
        log.info("AUDIT weekly digest event received: userId={}, weekKey={}", event.userId(), event.weekKey());
        weeklyDigestService.sendWeeklyDigest(event.userId(), event.weekKey());
    }
}
