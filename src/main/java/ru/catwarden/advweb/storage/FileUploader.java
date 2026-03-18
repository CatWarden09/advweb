package ru.catwarden.advweb.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.exception.FileOperationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
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
            Files.createDirectories(uploadDir);
        } catch (IOException e){
            throw new FileOperationException("Failed to create directory");
        }

        for(MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new FileOperationException("File too large: " + file.getOriginalFilename());
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new FileOperationException("Unsupported file type: " + file.getOriginalFilename());
            }

            try {
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path filePath = uploadDir.resolve(filename);

                file.transferTo(filePath.toFile());

                uploadedFiles.add(new StoredFile(filename, filePath.toString()));

            } catch (IOException e) {
                throw new FileOperationException("Failed to save the file");
            }
        }

        return uploadedFiles;
    }

    public StoredFile uploadFile(MultipartFile file, Path uploadDir){
        return uploadFiles(List.of(file), uploadDir).getFirst();
    }
}
