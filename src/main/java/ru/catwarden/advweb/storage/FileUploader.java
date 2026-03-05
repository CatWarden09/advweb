package ru.catwarden.advweb.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploader {
    public List<StoredFile> uploadFiles(List<MultipartFile> files, Path uploadDir){
        List<StoredFile> uploadedFiles = new ArrayList<>();

        try{
            Files.createDirectories(uploadDir);
        } catch (IOException e){
            throw new RuntimeException("Failed to create directory");
        }

        for(MultipartFile file : files) {
            try {
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path filePath = uploadDir.resolve(filename);

                file.transferTo(filePath.toFile());

                uploadedFiles.add(new StoredFile(filename, filePath.toString()));

            } catch (IOException e) {
                throw new RuntimeException("Failed to save the file", e);
            }
        }

        return uploadedFiles;
    }

    public StoredFile uploadFile(MultipartFile file, Path uploadDir){
        return uploadFiles(List.of(file), uploadDir).getFirst();
    }
}
