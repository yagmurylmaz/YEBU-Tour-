package com.hotel.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stores room photos outside the classpath (user home) so they persist across runs.
 */
public final class RoomImageStorage {

    private RoomImageStorage() {}

    public static Path getStorageDir() {
        return ImageStorageService.getStorageDir("room-images");
    }

    public static String copyToRoomFile(int roomId, File sourceFile) throws IOException {
        return ImageStorageService.copyToEntityFile("room-images", "room", roomId, sourceFile);
    }

    public static void deleteFileIfExists(String absolutePath) {
        ImageStorageService.deleteFileIfExists(absolutePath);
    }

    /**
     * Loads a JavaFX image from an absolute file path, or null if missing/invalid.
     */
    public static Image loadFxImage(String absolutePath) {
        return ImageStorageService.loadFxImage(absolutePath);
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return ".jpg";
        String ext = filename.substring(dot).toLowerCase();
        if (!ext.matches("\\.(jpg|jpeg|png|gif|bmp|webp)")) return ".jpg";
        return ext;
    }
}
