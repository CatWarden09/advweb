package ru.catwarden.advweb.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WeeklyDigestListenerTest {

    @Mock
    private WeeklyDigestService weeklyDigestService;

    @InjectMocks
    private WeeklyDigestListener weeklyDigestListener;

    @Test
    void handleWeeklyDigestDelegatesToService() {
        WeeklyDigestEvent event = new WeeklyDigestEvent(5L, "2026-W13");

        weeklyDigestListener.handleWeeklyDigest(event);

        verify(weeklyDigestService).sendWeeklyDigest(5L, "2026-W13");
    }
}
