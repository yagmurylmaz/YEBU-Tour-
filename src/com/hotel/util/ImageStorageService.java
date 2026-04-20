package com.hotel.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ImageStorageService {
    private ImageStorageService() {}

    public static String copyToEntityFile(String folderName, String prefix, int entityId, File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.isFile()) {
            throw new IllegalArgumentException("Select a valid image file.");
        }
        String ext = extensionOf(sourceFile.getName());
        Path dest = getStorageDir(folderName).resolve(prefix + "-" + entityId + "-" + System.nanoTime() + ext);
        Files.copy(sourceFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toAbsolutePath().toString().replace('\\', '/');
    }

    public static Path getStorageDir(String folderName) {
        Path dir = Path.of(System.getProperty("user.home"), ".hotel-app", folderName);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create image directory: " + dir, e);
        }
        return dir;
    }

    public static void deleteFileIfExists(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) return;
        if (isRemotePath(absolutePath)) return;
        try {
            Path path = normalizeToLocalPath(absolutePath);
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        } catch (Exception ignored) {
        }
    }

    public static Image loadFxImage(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) return null;
        try {
            String source = absolutePath.trim();
            if (isRemotePath(source)) {
                Image remote = new Image(source, true);
                return remote.isError() ? null : remote;
            }
            Path localPath = normalizeToLocalPath(source);
            File f = localPath.toFile();
            if (!f.isFile()) return null;
            Image local = new Image(f.toURI().toString(), true);
            return local.isError() ? null : local;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isRemotePath(String path) {
        String normalized = path.trim().toLowerCase();
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private static Path normalizeToLocalPath(String path) {
        String normalized = path.trim();
        if (normalized.toLowerCase().startsWith("file:")) {
            return Path.of(URI.create(normalized));
        }
        return Path.of(normalized);
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return ".jpg";
        String ext = filename.substring(dot).toLowerCase();
        if (!ext.matches("\\.(jpg|jpeg|png|gif|bmp|webp)")) return ".jpg";
        return ext;
    }
}

