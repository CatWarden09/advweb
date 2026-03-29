package ru.catwarden.advweb.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyDigestProducer {
    private final KafkaTemplate<String, WeeklyDigestEvent> kafkaTemplate;

    @Value("${app.kafka.topics.weekly-digest}")
    private String weeklyDigestTopic;

    public void send(WeeklyDigestEvent event) {
        kafkaTemplate.send(weeklyDigestTopic, event.userId().toString(), event);
        log.info("AUDIT weekly digest event published: userId={}, weekKey={}", event.userId(), event.weekKey());
    }
}
