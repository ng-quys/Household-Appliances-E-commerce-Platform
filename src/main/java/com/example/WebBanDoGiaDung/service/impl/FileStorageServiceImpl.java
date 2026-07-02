package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.service.FileStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path productUploadRoot = Paths.get("uploads", "products").toAbsolutePath().normalize();
    private final Path brandUploadRoot = Paths.get("uploads", "brands").toAbsolutePath().normalize();

    @Override
    public String storeProductImage(MultipartFile file) {
        return storeImage(file, productUploadRoot, "/uploads/products/");
    }

    @Override
    public String storeBrandImage(MultipartFile file) {
        return storeImage(file, brandUploadRoot, "/uploads/brands/");
    }

    private String storeImage(MultipartFile file, Path uploadRoot, String publicPrefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file ảnh hợp lệ.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Ảnh tải lên không được vượt quá 5MB.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, JPEG, PNG hoặc WEBP.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("File tải lên phải là ảnh JPG, JPEG, PNG hoặc WEBP.");
        }

        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadRoot);
            Path target = uploadRoot.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh tải lên.", exception);
        }

        return publicPrefix + fileName;
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
