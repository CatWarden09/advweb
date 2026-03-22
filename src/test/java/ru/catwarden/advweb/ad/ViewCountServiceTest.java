package ru.catwarden.advweb.ad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewCountServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private AdvertisementRepository advertisementRepository;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ViewCountService viewCountService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void incrementAddsIdToUpdateSetAndIncrementsCounter() {
        viewCountService.increment(12L);

        verify(setOperations).add("adv:view_updates", "12");
        verify(valueOperations).increment("adv:views:12");
    }

    @Test
    void syncViewsToDatabaseDoesNothingWhenNoIds() {
        when(setOperations.pop("adv:view_updates", 1000)).thenReturn(List.of());

        viewCountService.syncViewsToDatabase();

        verify(advertisementRepository, never()).incrementViews(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void syncViewsToDatabaseSavesPositiveCounter() {
        when(setOperations.pop("adv:view_updates", 1000)).thenReturn(List.of("9"));
        when(valueOperations.getAndDelete("adv:views:9")).thenReturn("5");

        viewCountService.syncViewsToDatabase();

        verify(advertisementRepository).incrementViews(9L, 5L);
    }

    @Test
    void syncViewsToDatabaseSkipsNullAndZeroCounters() {
        when(setOperations.pop("adv:view_updates", 1000)).thenReturn(List.of("1", "2"));
        when(valueOperations.getAndDelete("adv:views:1")).thenReturn(null);
        when(valueOperations.getAndDelete("adv:views:2")).thenReturn("0");

        viewCountService.syncViewsToDatabase();

        verify(advertisementRepository, never()).incrementViews(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void syncViewsToDatabaseContinuesWhenOneIdHasInvalidCounter() {
        when(setOperations.pop("adv:view_updates", 1000)).thenReturn(List.of("3", "4"));
        when(valueOperations.getAndDelete("adv:views:3")).thenReturn("not-a-number");
        when(valueOperations.getAndDelete("adv:views:4")).thenReturn("8");

        viewCountService.syncViewsToDatabase();

        verify(advertisementRepository).incrementViews(4L, 8L);
    }
}
