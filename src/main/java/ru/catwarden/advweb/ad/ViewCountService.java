package ru.catwarden.advweb.ad;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountService {

    private final StringRedisTemplate redisTemplate;
    private final AdvertisementRepository advertisementRepository;

    private static final String UPDATE_SET_KEY = "adv:view_updates";
    private static final String VIEW_COUNT_PREFIX = "adv:views:";

    public void increment(Long advertisementId) {
        String idStr = advertisementId.toString();
        redisTemplate.opsForSet().add(UPDATE_SET_KEY, idStr);
        redisTemplate.opsForValue().increment(VIEW_COUNT_PREFIX + idStr);
    }

    @Scheduled(fixedDelay = 60000)
    public void syncViewsToDatabase() {
        // Pop up to 1000 IDs to update in one batch
        List<String> idsToUpdate = redisTemplate.opsForSet().pop(UPDATE_SET_KEY, 1000);

        if (idsToUpdate == null || idsToUpdate.isEmpty()) {
            return;
        }

        log.info("Syncing {} advertisement view counts to database", idsToUpdate.size());

        for (String idStr : idsToUpdate) {
            try {
                String key = VIEW_COUNT_PREFIX + idStr;
                // Atomically get and delete the count from Redis
                String countStr = redisTemplate.opsForValue().getAndDelete(key);

                if (countStr != null) {
                    long count = Long.parseLong(countStr);
                    if (count > 0) {
                        advertisementRepository.incrementViews(Long.parseLong(idStr), count);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to sync views for advertisement id: {}", idStr, e);
            }
        }
    }
}
