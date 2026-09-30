package com.backendintranet.service.impl;

import com.backendintranet.exception.BadRequestException;
import com.backendintranet.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AbsenceSupportStorage {

    private static final long MAX_SIZE =
            5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/jpg",
                    "image/png");

    @Value("${absence.support.dir:private-uploads/absences}")
    private String supportDir;

    public StoredSupport store(
            MultipartFile file) {

        if (file == null
                || file.isEmpty()) {

            return null;
        }

        if (file.getSize() > MAX_SIZE) {

            throw new BadRequestException(
                    "El soporte no debe superar 5 MB");
        }

        String original =
                file.getOriginalFilename();

        if (original == null
                || original.isBlank()) {

            throw new BadRequestException(
                    "El archivo de soporte no tiene un nombre válido");
        }

        String originalExtension =
                extension(original);

        if (!Set.of(
                        "jpg",
                        "jpeg",
                        "png")
                .contains(
                        originalExtension)) {

            throw new BadRequestException(
                    "El soporte debe ser una imagen JPG, JPEG o PNG");
        }

        String declaredContentType =
                file.getContentType();

        if (declaredContentType == null
                || !ALLOWED_CONTENT_TYPES.contains(
                        declaredContentType
                                .toLowerCase(
                                        Locale.ROOT))) {

            throw new BadRequestException(
                    "El tipo de contenido del soporte no es válido");
        }

        DetectedImage detected =
                detectImage(file);

        boolean extensionMatches =
                switch (detected.extension()) {
                    case "jpg" ->
                            originalExtension.equals("jpg")
                                    || originalExtension.equals("jpeg");
                    case "png" ->
                            originalExtension.equals("png");
                    default -> false;
                };

        if (!extensionMatches) {

            throw new BadRequestException(
                    "La extensión del soporte no coincide con el contenido real");
        }

        boolean contentTypeMatches =
                switch (detected.contentType()) {
                    case "image/jpeg" ->
                            declaredContentType
                                            .equalsIgnoreCase(
                                                    "image/jpeg")
                                    || declaredContentType
                                            .equalsIgnoreCase(
                                                    "image/jpg");
                    case "image/png" ->
                            declaredContentType
                                    .equalsIgnoreCase(
                                            "image/png");
                    default -> false;
                };

        if (!contentTypeMatches) {

            throw new BadRequestException(
                    "El tipo declarado del soporte no coincide con el contenido real");
        }

        Path base =
                basePath();

        String storedName =
                UUID.randomUUID()
                        + "."
                        + detected.extension();

        Path target =
                safeResolve(
                        base,
                        storedName);

        try {

            Files.createDirectories(
                    base);

            Files.copy(
                    file.getInputStream(),
                    target);

        } catch (IOException ex) {

            throw new BadRequestException(
                    "No fue posible guardar el soporte de la ausencia");
        }

        return new StoredSupport(
                storedName,
                detected.contentType());
    }

    public Resource load(
            String storedName) {

        if (storedName == null
                || storedName.isBlank()
                || !Paths.get(storedName)
                        .getFileName()
                        .toString()
                        .equals(storedName)) {

            throw new ResourceNotFoundException(
                    "Soporte no encontrado");
        }

        Path target =
                safeResolve(
                        basePath(),
                        storedName);

        if (!Files.isRegularFile(target)) {

            throw new ResourceNotFoundException(
                    "Soporte no encontrado");
        }

        try {

            Resource resource =
                    new UrlResource(
                            target.toUri());

            if (!resource.exists()
                    || !resource.isReadable()) {

                throw new ResourceNotFoundException(
                        "Soporte no encontrado");
            }

            return resource;

        } catch (MalformedURLException ex) {

            throw new ResourceNotFoundException(
                    "Soporte no encontrado");
        }
    }

    public String contentType(
            String storedName) {

        String ext =
                extension(
                        storedName);

        return switch (ext) {
            case "jpg", "jpeg" ->
                    "image/jpeg";
            case "png" ->
                    "image/png";
            default ->
                    "application/octet-stream";
        };
    }

    public String extensionOf(
            String storedName) {

        return extension(
                storedName);
    }

    public void deleteQuietly(
            String storedName) {

        if (storedName == null
                || storedName.isBlank()) {

            return;
        }

        try {

            Files.deleteIfExists(
                    safeResolve(
                            basePath(),
                            storedName));

        } catch (Exception ignored) {
            // Limpieza de rollback: no debe ocultar
            // la excepción original de la transacción.
        }
    }

    private DetectedImage detectImage(
            MultipartFile file) {

        byte[] header =
                new byte[8];

        int read;

        try (InputStream in =
                file.getInputStream()) {

            read =
                    in.read(header);

        } catch (IOException ex) {

            throw new BadRequestException(
                    "No fue posible validar el soporte");
        }

        if (read >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {

            return new DetectedImage(
                    "jpg",
                    "image/jpeg");
        }

        if (read >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {

            return new DetectedImage(
                    "png",
                    "image/png");
        }

        throw new BadRequestException(
                "El soporte no contiene una imagen JPG o PNG válida");
    }

    private Path basePath() {

        return Paths.get(
                        supportDir)
                .toAbsolutePath()
                .normalize();
    }

    private Path safeResolve(
            Path base,
            String storedName) {

        Path target =
                base.resolve(
                                storedName)
                        .normalize();

        if (!target.startsWith(base)) {

            throw new BadRequestException(
                    "Ruta de soporte inválida");
        }

        return target;
    }

    private String extension(
            String filename) {

        int index =
                filename.lastIndexOf('.');

        if (index < 0
                || index
                        == filename.length() - 1) {

            return "";
        }

        return filename
                .substring(index + 1)
                .toLowerCase(
                        Locale.ROOT);
    }

    public record StoredSupport(
            String storedName,
            String contentType) {}

    private record DetectedImage(
            String extension,
            String contentType) {}
}
