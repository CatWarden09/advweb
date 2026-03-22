package ru.catwarden.advweb.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.catwarden.advweb.exception.FileStorageException;
import ru.catwarden.advweb.exception.FileTooLargeException;
import ru.catwarden.advweb.exception.InvalidFileTypeException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploaderTest {

    private final FileUploader fileUploader = new FileUploader();

    @TempDir
    Path tempDir;

    @Test
    void uploadFilesSavesAllowedFilesAndReturnsMetadata() throws IOException {
        Path uploadDir = tempDir.resolve("uploads");
        MultipartFile file1 = new MockMultipartFile(
                "files",
                "a.png",
                "image/png",
                "first".getBytes(StandardCharsets.UTF_8)
        );
        MultipartFile file2 = new MockMultipartFile(
                "files",
                "b.jpg",
                "image/jpeg",
                "second".getBytes(StandardCharsets.UTF_8)
        );

        List<StoredFile> result = fileUploader.uploadFiles(List.of(file1, file2), uploadDir);

        assertEquals(2, result.size());
        assertTrue(result.get(0).getFilename().endsWith("_a.png"));
        assertTrue(result.get(1).getFilename().endsWith("_b.jpg"));
        assertTrue(Files.exists(Path.of(result.get(0).getPath())));
        assertTrue(Files.exists(Path.of(result.get(1).getPath())));
    }

    @Test
    void uploadFileReturnsSingleStoredFile() {
        Path uploadDir = tempDir.resolve("single");
        MultipartFile file = new MockMultipartFile(
                "file",
                "avatar.webp",
                "image/webp",
                "avatar".getBytes(StandardCharsets.UTF_8)
        );

        StoredFile result = fileUploader.uploadFile(file, uploadDir);

        assertTrue(result.getFilename().endsWith("_avatar.webp"));
        assertTrue(Files.exists(Path.of(result.getPath())));
    }

    @Test
    void uploadFilesThrowsWhenFileTooLarge() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(50L * 1024L * 1024L + 1L);
        when(file.getOriginalFilename()).thenReturn("big.png");

        assertThrows(
                FileTooLargeException.class,
                () -> fileUploader.uploadFiles(List.of(file), tempDir.resolve("too-big"))
        );

        verify(file, never()).transferTo(any(File.class));
    }

    @Test
    void uploadFilesThrowsWhenFileTypeUnsupported() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("doc.pdf");

        assertThrows(
                InvalidFileTypeException.class,
                () -> fileUploader.uploadFiles(List.of(file), tempDir.resolve("bad-type"))
        );
    }

    @Test
    void uploadFilesThrowsWhenFileTypeIsNull() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn(null);
        when(file.getOriginalFilename()).thenReturn("unknown.bin");

        assertThrows(
                InvalidFileTypeException.class,
                () -> fileUploader.uploadFiles(List.of(file), tempDir.resolve("null-type"))
        );
    }

    @Test
    void uploadFilesThrowsWhenTransferFails() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("broken.png");
        doThrow(new IOException("disk error")).when(file).transferTo(any(File.class));

        assertThrows(
                FileStorageException.class,
                () -> fileUploader.uploadFiles(List.of(file), tempDir.resolve("io-error"))
        );
    }
}
