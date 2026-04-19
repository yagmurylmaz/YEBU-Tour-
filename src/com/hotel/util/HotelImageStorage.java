package com.hotel.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class HotelImageStorage {
    private HotelImageStorage() {}

    public static Path getStorageDir() {
        return ImageStorageService.getStorageDir("hotel-images");
    }

    public static String copyToHotelFile(int hotelId, File sourceFile) throws IOException {
        return ImageStorageService.copyToEntityFile("hotel-images", "hotel", hotelId, sourceFile);
    }

    public static void deleteFileIfExists(String absolutePath) {
        ImageStorageService.deleteFileIfExists(absolutePath);
    }

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

