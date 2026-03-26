package ru.catwarden.advweb.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.exception.FileOperationException;
import ru.catwarden.advweb.exception.FileStorageException;
import ru.catwarden.advweb.exception.FileTooLargeException;
import ru.catwarden.advweb.exception.InvalidFileTypeException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploader {
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    public List<StoredFile> uploadFiles(List<MultipartFile> files, Path uploadDir){
        List<StoredFile> uploadedFiles = new ArrayList<>();

        try{
            if (!Files.exists(uploadDir)) {
                log.info("Creating directory for uploads: {}", uploadDir);
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e){
            log.error("CRITICAL: Failed to create directory: {}", uploadDir, e);
            throw new FileStorageException("Failed to create directory");
        }

        for(MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE) {
                log.warn("File too large: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
                throw new FileTooLargeException("File too large: " + file.getOriginalFilename());
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                log.warn("Unsupported file type: {} ({})", file.getOriginalFilename(), contentType);
                throw new InvalidFileTypeException("Unsupported file type: " + file.getOriginalFilename());
            }

            try {
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path filePath = uploadDir.resolve(filename);

                file.transferTo(filePath.toFile());
                log.info("File saved: {} as {}", file.getOriginalFilename(), filename);

                uploadedFiles.add(new StoredFile(filename, filePath.toString()));

            } catch (IOException e) {
                log.error("Failed to save the file: {}", file.getOriginalFilename(), e);
                throw new FileStorageException("Failed to save the file");
            }
        }

        return uploadedFiles;
    }

    public StoredFile uploadFile(MultipartFile file, Path uploadDir){
        return uploadFiles(List.of(file), uploadDir).getFirst();
    }
}
