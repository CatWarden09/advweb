package ru.catwarden.advweb.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.ad.Advertisement;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.enums.Status;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyDigestService {
    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;
    private final WeeklyDigestLogRepository weeklyDigestLogRepository;
    private final JavaMailSender javaMailSender;

    @Value("${app.weekly-digest.mail-from}")
    private String fromEmail;

    @Value("${app.public.base-url}")
    private String publicBaseUrl;

    public void sendWeeklyDigest(Long userId, String weekKey) {
        if (weeklyDigestLogRepository.existsByUserIdAndWeekKey(userId, weekKey)) {
            log.info("Weekly digest already sent: userId={}, weekKey={}", userId, weekKey);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, userId));

        List<Advertisement> topAdvertisements =
                advertisementRepository.findTop5ByAuthorIdAndStatusOrderByViewsDesc(userId, Status.APPROVED);

        if (topAdvertisements.isEmpty()) {
            log.info("Weekly digest skipped because no approved advertisements were found: userId={}, weekKey={}",
                    userId, weekKey);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("Weekly digest for your advertisements");
        message.setText(buildEmailBody(user, topAdvertisements, weekKey));

        javaMailSender.send(message);

        try {
            weeklyDigestLogRepository.save(WeeklyDigestLog.builder()
                    .userId(userId)
                    .weekKey(weekKey)
                    .sentAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException exception) {
            log.info("Weekly digest log already exists after send attempt: userId={}, weekKey={}", userId, weekKey);
            return;
        }

        log.info("AUDIT weekly digest email sent: userId={}, weekKey={}, advertisementCount={}",
                userId, weekKey, topAdvertisements.size());
    }

    private String buildEmailBody(User user, List<Advertisement> advertisements, String weekKey) {
        StringBuilder body = new StringBuilder();
        body.append("Здравствуйте, ").append(user.getFirstName()).append("!\n\n");
        body.append("Вот ваш еженедельный топ-5 объявлений по просмотрам за ").append(weekKey).append(":\n\n");

        for (int i = 0; i < advertisements.size(); i++) {
            Advertisement advertisement = advertisements.get(i);
            body.append(i + 1)
                    .append(". ")
                    .append(advertisement.getName())
                    .append(" - ")
                    .append(advertisement.getViews())
                    .append(" просмотров, цена ")
                    .append(advertisement.getPrice())
                    .append("\n")
                    .append(publicBaseUrl)
                    .append("/advertisements/")
                    .append(advertisement.getId())
                    .append("\n\n");
        }

        body.append("С уважением,\nAdvweb");
        return body.toString();
    }
}
