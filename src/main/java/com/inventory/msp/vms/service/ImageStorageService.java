package com.inventory.msp.vms.service;

import com.inventory.msp.vms.config.VmsTmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageStorageService {

    private final VmsTmsProperties properties;

    public String saveDecodedBase64Image(String base64String, String originalFileName, Integer serverId, String channelId) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return null;
        }

        try {
            // Remove data URI scheme prefix if present (e.g. data:image/jpeg;base64,...)
            String cleanedBase64 = base64String.contains(",")
                    ? base64String.substring(base64String.indexOf(",") + 1)
                    : base64String;

            byte[] imageBytes = Base64.getDecoder().decode(cleanedBase64.trim());

            String subDirectory = String.format("%s/%d/%s",
                    properties.getStorageDirectory(),
                    serverId,
                    channelId != null ? channelId : "general"
            );
            Path dirPath = Paths.get(subDirectory);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String fileExtension = ".jpg";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String generatedFileName = UUID.randomUUID() + fileExtension;
            Path filePath = dirPath.resolve(generatedFileName);

            try (OutputStream out = new FileOutputStream(filePath.toFile())) {
                out.write(imageBytes);
            }

            log.debug("Saved decoded image to: {}", filePath);
            return filePath.toString().replace("\\", "/");
        } catch (Exception e) {
            log.error("Failed to decode and save base64 image: {}", e.getMessage(), e);
            return null;
        }
    }
}
