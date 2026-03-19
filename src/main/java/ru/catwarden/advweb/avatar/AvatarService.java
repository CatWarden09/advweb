package ru.catwarden.advweb.avatar;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.avatar.dto.AvatarDto;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.FileOperationException;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.storage.FileUploader;
import ru.catwarden.advweb.storage.StoredFile;
import ru.catwarden.advweb.user.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
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

        return avatarMapper.toDto(avatar);
    }

    public AvatarDto getUserAvatar(Long userId){
        Avatar avatar = avatarRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, userId));

        return avatarMapper.toDto(avatar);
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
                });

        avatar.setLinkedToUser(true);
        avatar.setUserId(userId);

        avatarRepository.save(avatar);
    }



    public void unlinkUserAvatar(Long userId){
        Avatar avatar = avatarRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, userId));

        avatar.setLinkedToUser(false);
        avatar.setUserId(null);

        avatarRepository.save(avatar);
    }

    private void validateAvatarCanBeLinked(Avatar avatar, Long userId) {
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        boolean isLinkedToAnotherUser = Boolean.TRUE.equals(avatar.getLinkedToUser())
                && !userId.equals(avatar.getUserId());
        if (isLinkedToAnotherUser) {
            throw new AccessDeniedException("Avatar is linked to another user");
        }

        boolean isUploadedByAnotherUser = !isAdmin
                && !currentKeycloakId.equals(avatar.getUploaderKeycloakId())
                && !userId.equals(avatar.getUserId());
        if (isUploadedByAnotherUser) {
            throw new AccessDeniedException("Avatar was uploaded by another user");
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
            throw new FileOperationException("Failed to delete unused images");
        }

        avatarRepository.deleteAll(avatars);
    }
}
