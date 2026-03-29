package ru.catwarden.advweb.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import ru.catwarden.advweb.ad.Advertisement;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.enums.Status;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyDigestServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AdvertisementRepository advertisementRepository;
    @Mock
    private WeeklyDigestLogRepository weeklyDigestLogRepository;
    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private WeeklyDigestService weeklyDigestService;

    @Test
    void sendWeeklyDigestReturnsWhenLogAlreadyExists() {
        when(weeklyDigestLogRepository.existsByUserIdAndWeekKey(5L, "2026-W13")).thenReturn(true);

        weeklyDigestService.sendWeeklyDigest(5L, "2026-W13");

        verify(userRepository, never()).findById(any());
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendWeeklyDigestSkipsWhenNoApprovedAdvertisementsFound() {
        User user = User.builder()
                .id(5L)
                .firstName("Ivan")
                .email("ivan@example.com")
                .build();

        when(weeklyDigestLogRepository.existsByUserIdAndWeekKey(5L, "2026-W13")).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(advertisementRepository.findTop5ByAuthorIdAndStatusOrderByViewsDesc(5L, Status.APPROVED))
                .thenReturn(List.of());

        weeklyDigestService.sendWeeklyDigest(5L, "2026-W13");

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
        verify(weeklyDigestLogRepository, never()).save(any());
    }

    @Test
    void sendWeeklyDigestSendsEmailAndSavesLog() {
        User user = User.builder()
                .id(5L)
                .firstName("Ivan")
                .email("ivan@example.com")
                .build();
        Advertisement firstAdvertisement = Advertisement.builder()
                .id(100L)
                .name("Bike")
                .price(350.0)
                .views(120L)
                .build();
        Advertisement secondAdvertisement = Advertisement.builder()
                .id(101L)
                .name("Laptop")
                .price(900.0)
                .views(90L)
                .build();

        ReflectionTestUtils.setField(weeklyDigestService, "fromEmail", "no-reply@advweb.local");
        ReflectionTestUtils.setField(weeklyDigestService, "publicBaseUrl", "https://advweb.example");

        when(weeklyDigestLogRepository.existsByUserIdAndWeekKey(5L, "2026-W13")).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(advertisementRepository.findTop5ByAuthorIdAndStatusOrderByViewsDesc(5L, Status.APPROVED))
                .thenReturn(List.of(firstAdvertisement, secondAdvertisement));

        weeklyDigestService.sendWeeklyDigest(5L, "2026-W13");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());
        verify(weeklyDigestLogRepository).save(any(WeeklyDigestLog.class));

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("no-reply@advweb.local", sentMessage.getFrom());
        assertEquals("ivan@example.com", sentMessage.getTo()[0]);
        assertEquals("Weekly digest for your advertisements", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("Bike"));
        assertTrue(sentMessage.getText().contains("https://advweb.example/advertisements/100"));
        assertTrue(sentMessage.getText().contains("Laptop"));
    }
}
