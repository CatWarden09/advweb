package ru.catwarden.advweb.avatar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.avatar.dto.AvatarDto;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.FileOperationException;
import ru.catwarden.advweb.exception.FileStorageException;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.storage.FileUploader;
import ru.catwarden.advweb.storage.StoredFile;
import ru.catwarden.advweb.user.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarService {
    private final AvatarRepository avatarRepository;
    private final AvatarMapper avatarMapper;

    private final FileUploader fileUploader;

    private final Path uploadDir = Paths.get("/app/uploads/user_avatars/");
    private final String fileUrl = "/uploads/user_avatars/";

    public AvatarDto uploadAvatar(MultipartFile file){
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        StoredFile uploadedFile = fileUploader.uploadFile(file, uploadDir);

        Avatar avatar = new Avatar();

        avatar.setPath(uploadedFile.getPath());
        avatar.setUrl(fileUrl + uploadedFile.getFilename());
        avatar.setUploaderKeycloakId(currentKeycloakId);
        avatar.setLinkedToUser(false);

        avatarRepository.save(avatar);

        log.info(
                "AUDIT new avatar uploaded: url={}, name={}, uploaderKeycloakId={}",
                avatar.getUrl(),
                uploadedFile.getFilename(),
                currentKeycloakId
        );

        return avatarMapper.toDto(avatar);
    }

    public AvatarDto getUserAvatar(Long userId){
        Avatar avatar = avatarRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, userId));

        return avatarMapper.toDto(avatar);
    }

    public Optional<String> findUserAvatarUrl(Long userId) {
        return avatarRepository.findByUserId(userId)
                .map(Avatar::getUrl);
    }

    @Transactional
    public void setAvatarToUser(Long avatarId, Long userId){
        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new EntityNotFoundException(Avatar.class, avatarId));
        validateAvatarCanBeLinked(avatar, userId);

        avatarRepository.findByUserId(userId)
                .filter(currentAvatar -> !currentAvatar.getId().equals(avatarId))
                .ifPresent(currentAvatar -> {
                    currentAvatar.setLinkedToUser(false);
                    currentAvatar.setUserId(null);
                    avatarRepository.save(currentAvatar);
                    log.info("AUDIT --(set new avatar to user)-- Current avatar unlinked: avatarId={}, userId={}, actorId={}",
                            avatarId,
                            userId,
                            SecurityUtils.getCurrentUserKeycloakId());
                });

        avatar.setLinkedToUser(true);
        avatar.setUserId(userId);

        log.info("AUDIT --(set new avatar to user)-- New avatar linked: avatarId={}, userId={}, actorId={}",
                avatarId,
                userId,
                SecurityUtils.getCurrentUserKeycloakId());

        avatarRepository.save(avatar);
    }



    public void unlinkUserAvatar(Long userId){
        Avatar avatar = avatarRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, userId));

        avatar.setLinkedToUser(false);
        avatar.setUserId(null);

        log.info("AUDIT avatar unlinked: avatarId={}, userId={}, actorId={}",
                avatar.getId(),
                userId,
                SecurityUtils.getCurrentUserKeycloakId());

        avatarRepository.save(avatar);
    }

    private void validateAvatarCanBeLinked(Avatar avatar, Long userId) {
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        boolean isLinkedToAnotherUser = Boolean.TRUE.equals(avatar.getLinkedToUser())
                && !userId.equals(avatar.getUserId());
        if (isLinkedToAnotherUser) {
            throw new DetailedAccessDeniedException("Avatar is linked to another user",
                    Map.of(
                            "Avatar id:", avatar.getId(),
                            "Requested user id:", userId,
                            "Avatar is already linked:", true
                    ));
        }

        boolean isUploadedByAnotherUser = !isAdmin
                && !currentKeycloakId.equals(avatar.getUploaderKeycloakId())
                && !userId.equals(avatar.getUserId());
        if (isUploadedByAnotherUser) {
            throw new DetailedAccessDeniedException("Avatar was uploaded by another user",
                    Map.of(
                            "Avatar id:", avatar.getId(),
                            "Avatar uploader keycloak id:", String.valueOf(avatar.getUploaderKeycloakId()),
                            "Requested user id:", userId
                    ));
        }
    }

@Scheduled(cron = "0 0 5 * * *", zone = "Europe/Moscow")
public void deleteUnusedAvatars(){
    List<Avatar> avatars = avatarRepository.findAllByLinkedToUserFalse();

    try{
        for(Avatar avatar : avatars){
            Files.deleteIfExists(Paths.get(avatar.getPath()));
        }
    } catch (IOException e){
        throw new FileStorageException("Failed to delete unused images");
    }

    avatarRepository.deleteAll(avatars);
}
}
