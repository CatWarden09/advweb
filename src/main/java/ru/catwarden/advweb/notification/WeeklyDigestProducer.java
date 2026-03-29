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
        kafkaTemplate.send(weeklyDigestTopic, event.userId().toString(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Weekly digest event publish failed: userId={}, weekKey={}",
                                event.userId(),
                                event.weekKey(),
                                exception);
                        return;
                    }

                    log.info("AUDIT weekly digest event published: userId={}, weekKey={}, topic={}, partition={}, offset={}",
                            event.userId(),
                            event.weekKey(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}
