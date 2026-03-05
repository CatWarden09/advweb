package ru.catwarden.advweb.avatar;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.avatar.dto.AvatarDto;
import ru.catwarden.advweb.storage.FileUploader;
import ru.catwarden.advweb.storage.StoredFile;

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
        StoredFile uploadedFile = fileUploader.uploadFile(file, uploadDir);

        Avatar avatar = new Avatar();

        avatar.setPath(uploadedFile.getPath());
        avatar.setUrl(fileUrl + uploadedFile.getFilename());
        avatar.setLinkedToUser(false);

        avatarRepository.save(avatar);

        return avatarMapper.toDto(avatar);
    }

    public AvatarDto getUserAvatar(Long userId){
        Avatar avatar = avatarRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Avatar with user id " + userId + " not found"));

        return avatarMapper.toDto(avatar);
    }

    public void setUserAvatar(Long avatarId, Long userId){
        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new RuntimeException("Avatar not found"));


        avatar.setLinkedToUser(true);
        avatar.setUserId(userId);

        avatarRepository.save(avatar);
    }

    public void unlinkUserAvatar(Long userId){
        Avatar avatar = avatarRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Avatar with user id " + userId + " not found"));

        avatar.setLinkedToUser(false);
        avatar.setUserId(null);

        avatarRepository.save(avatar);
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Europe/Moscow")
    public void deleteUnusedAvatars(){
        List<Avatar> avatars = avatarRepository.findAllByLinkedToUserFalse();

        try{
            for(Avatar avatar : avatars){
                Files.deleteIfExists(Paths.get(avatar.getPath()));
            }
        } catch (IOException e){
            throw new RuntimeException("Failed to delete unused images");
        }

        avatarRepository.deleteAll(avatars);
    }
}
