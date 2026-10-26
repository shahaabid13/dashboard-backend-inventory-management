package com.inventory.msp.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class FileStorageService {

    private final Path uploadDir;
    private final long maxFileSizeBytes;

    public FileStorageService(
            @Value("${app.upload.dir:${APP_UPLOAD_DIR:/var/app-uploads/attachments}}") String uploadDir,
            @Value("${app.upload.max-file-size-bytes:${APP_UPLOAD_MAX_FILE_SIZE_BYTES:1572864}}") long maxFileSizeBytes) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create upload directory: " + this.uploadDir, e);
        }
    }

    public StoredImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is required");
        }

        String mediaType = file.getContentType();
        if (mediaType == null || !mediaType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ResponseStatusException(BAD_REQUEST, "Only image files are allowed");
        }

        BufferedImage image;
        try {
            image = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid image file", e);
        }
        if (image == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid image file");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        double scale = Math.min(1.0d, 1600.0d / Math.max(width, height));
        int targetWidth = width;
        int targetHeight = height;
        if (scale < 1.0d) {
            targetWidth = Math.max(1, (int) Math.round(width * scale));
            targetHeight = Math.max(1, (int) Math.round(height * scale));
        }

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        byte[] compressedBytes;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            if (writer == null) {
                throw new ResponseStatusException(BAD_REQUEST, "JPEG writer not available");
            }

            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(0.8f);
            }

            try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
                writer.setOutput(imageOutputStream);
                writer.write(null, new IIOImage(resized, null, null), writeParam);
                writer.dispose();
            }
            compressedBytes = outputStream.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Unable to compress image", e);
        }

        if (compressedBytes.length > maxFileSizeBytes) {
            throw new ResponseStatusException(BAD_REQUEST, "Image too large even after compression");
        }

        String storedFileName = UUID.randomUUID() + ".jpg";
        Path destination = uploadDir.resolve(storedFileName).normalize();
        if (!destination.startsWith(uploadDir)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid storage path");
        }

        try {
            Files.write(destination, compressedBytes);
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Unable to save uploaded image", e);
        }

        return new StoredImage(storedFileName, "image/jpeg", compressedBytes.length);
    }

    public Resource loadAsResource(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid attachment file name");
        }

        Path target = uploadDir.resolve(storedFileName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid attachment file name");
        }

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(NOT_FOUND, "Attachment not found");
        }

        return new FileSystemResource(target);
    }

    public void deleteFile(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        Path target = uploadDir.resolve(storedFileName).normalize();
        if (!target.startsWith(uploadDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // no-op; DB record will still be removed and caller can retry cleanup
        }
    }

    public record StoredImage(String storedFileName, String contentType, long fileSizeBytes) {
    }
}
