package ru.catwarden.advweb.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.enums.Status;
import ru.catwarden.advweb.user.User;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyDigestSchedulerTest {

    @Mock
    private AdvertisementRepository advertisementRepository;

    @Mock
    private WeeklyDigestProducer weeklyDigestProducer;

    @InjectMocks
    private WeeklyDigestScheduler weeklyDigestScheduler;

    @Test
    void scheduleWeeklyDigestPublishesEventForEachApprovedAuthor() {
        User firstUser = User.builder().id(9L).build();
        User secondUser = User.builder().id(10L).build();

        when(advertisementRepository.findDistinctAuthorsByStatus(Status.APPROVED))
                .thenReturn(List.of(firstUser, secondUser));

        weeklyDigestScheduler.scheduleWeeklyDigest();

        ArgumentCaptor<WeeklyDigestEvent> eventCaptor = ArgumentCaptor.forClass(WeeklyDigestEvent.class);
        verify(weeklyDigestProducer, org.mockito.Mockito.times(2)).send(eventCaptor.capture());

        List<WeeklyDigestEvent> events = eventCaptor.getAllValues();
        String expectedWeekKey = weeklyDigestScheduler.buildWeekKey(LocalDate.now());

        assertEquals(new WeeklyDigestEvent(9L, expectedWeekKey), events.get(0));
        assertEquals(new WeeklyDigestEvent(10L, expectedWeekKey), events.get(1));
    }

    @Test
    void buildWeekKeyReturnsIsoWeekBasedValue() {
        String weekKey = weeklyDigestScheduler.buildWeekKey(LocalDate.of(2026, 3, 29));

        assertEquals("2026-W13", weekKey);
    }
}
