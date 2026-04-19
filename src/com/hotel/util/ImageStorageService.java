package com.hotel.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
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
        try {
            Files.deleteIfExists(Path.of(absolutePath));
        } catch (IOException ignored) {
        }
    }

    public static Image loadFxImage(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) return null;
        File f = new File(absolutePath);
        if (!f.isFile()) return null;
        try {
            return new Image(f.toURI().toString(), true);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return ".jpg";
        String ext = filename.substring(dot).toLowerCase();
        if (!ext.matches("\\.(jpg|jpeg|png|gif|bmp|webp)")) return ".jpg";
        return ext;
    }
}

