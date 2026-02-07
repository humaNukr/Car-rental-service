package com.example.carrental.service.abstracts;

import com.example.carrental.exception.file.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@RequiredArgsConstructor
public abstract class ImageService {

    private final String directory;

    protected String uploadImage(MultipartFile file, String subFolder) {
        if (file.isEmpty()) {
            throw new FileStorageException("File is empty");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            throw new FileStorageException("Filename contains invalid path sequence " + originalFilename);
        }

        File dir = new File(directory, subFolder);

        if (!dir.exists() && !dir.mkdirs()) {
            throw new FileStorageException("Could not create directory: " + dir.getAbsolutePath());
        }

        String extension = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf(".")))
                .orElse(".jpg");

        String uniqueFileName = UUID.randomUUID() + extension;
        Path filePath = dir.toPath().resolve(uniqueFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Could not store file " + uniqueFileName, e);
        }

        return uniqueFileName;
    }

    protected List<String> getImagesPaths(String subFolder) {
        Path dir = Path.of(directory, subFolder);

        if (!Files.exists(dir)) {
            throw new FileStorageException("Directory not found: " + dir.toAbsolutePath());
        }

        List<Path> images;
        try (Stream<Path> files = Files.list(dir)) {
            images = files.toList();
            if (images.isEmpty()) {
                throw new FileStorageException("No images found in directory " + dir.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new FileStorageException("Error reading image from " + dir.toAbsolutePath(), e);
        }

        return images.stream()
                .filter(Files::isRegularFile)
                .map(filePath -> "/images/" + subFolder + "/" + filePath.getFileName().toString())
                .toList();
    }

    protected void deleteImage(String baseDir, String imageUrl) {
        Path dir = Path.of(baseDir, Paths.get(imageUrl).getParent().toString());

        if (!Files.exists(dir)) {
            throw new FileStorageException("Directory not found: " + dir.toAbsolutePath());
        }

        try {
            Files.deleteIfExists(Paths.get(baseDir, imageUrl));
        } catch (IOException e) {
            throw new FileStorageException("Could not delete image " + imageUrl, e);
        }
    }

    protected void deleteFolder(String subfolder) {
        Path dir = Path.of(directory, subfolder);

        if (!Files.exists(dir)) {
            throw new FileStorageException("Directory not found: " + dir.toAbsolutePath());
        }
        try {
            FileSystemUtils.deleteRecursively(dir);
        } catch (IOException e) {
            throw new FileStorageException("Could not delete folder " + dir.toAbsolutePath(), e);
        }
    }

}
