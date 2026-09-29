package com.ecobridge.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class FirebaseStorageService {
    private static final long MAX_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final String bucketName;
    private final Path localUploadRoot;

    public FirebaseStorageService(@Value("${FIREBASE_STORAGE_BUCKET:}") String bucketName,
                                  @Value("${ecobridge.upload-dir:data/uploads}") String localUploadDir) {
        this.bucketName = bucketName;
        this.localUploadRoot = Path.of(localUploadDir).toAbsolutePath().normalize();
    }

    public Map<String, Object> upload(MultipartFile file, Long userId, String purpose) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an image to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be 5 MB or smaller");
        }
        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG and WebP images are supported");
        }
        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String safePurpose = Set.of("posts", "pickups").contains(purpose) ? purpose : "uploads";
        String objectName = safePurpose + "/" + userId + "/" + UUID.randomUUID() + extension;
        String downloadToken = UUID.randomUUID().toString();

        try {
            if (bucketName.isBlank()) return saveLocally(file, userId, safePurpose, extension, contentType);
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName))
                    .setContentType(contentType)
                    .setMetadata(Map.of("firebaseStorageDownloadTokens", downloadToken))
                    .build();
            StorageClient.getInstance().bucket().getStorage().create(blobInfo, file.getBytes());
            String encoded = URLEncoder.encode(objectName, StandardCharsets.UTF_8).replace("+", "%20");
            String url = "https://firebasestorage.googleapis.com/v0/b/" + bucketName
                    + "/o/" + encoded + "?alt=media&token=" + downloadToken;
            return Map.of("url", url, "contentType", contentType, "size", file.getSize(), "storage", "firebase");
        } catch (Exception exception) {
            return saveLocally(file, userId, safePurpose, extension, contentType);
        }
    }

    private Map<String, Object> saveLocally(MultipartFile file, Long userId, String purpose,
                                             String extension, String contentType) {
        String fileName = UUID.randomUUID() + extension;
        Path directory = localUploadRoot.resolve(purpose).resolve(String.valueOf(userId)).normalize();
        if (!directory.startsWith(localUploadRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload path");
        }
        try {
            Files.createDirectories(directory);
            file.transferTo(directory.resolve(fileName));
            String url = "/uploads/" + purpose + "/" + userId + "/" + fileName;
            return Map.of("url", url, "contentType", contentType, "size", file.getSize(), "storage", "local");
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not save image. Please try again.", exception);
        }
    }
}
