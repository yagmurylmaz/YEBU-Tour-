package com.hotel.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ImageStorageService {
    private static final int IMAGE_CACHE_MAX = 400;
    private static final Duration MISS_CACHE_TTL = Duration.ofSeconds(45);
    private static final int MAX_PARENT_WALK = 12;
    private static final Map<String, Image> IMAGE_CACHE = createImageCache();
    private static final Map<String, Instant> MISS_CACHE = createMissCache();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();

    private ImageStorageService() {}

    public static String copyToEntityFile(String folderName, String prefix, int entityId, File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.isFile()) {
            throw new IllegalArgumentException("Select a valid image file.");
        }
        return uploadToCloudinary(folderName, prefix, entityId, sourceFile.toPath());
    }

    public static Path getStorageDir(String folderName) {
        return Path.of(System.getProperty("user.home"), ".hotel-app", folderName);
    }

    public static Path getSharedStorageDir(String folderName) {
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize().resolve(folderName);
    }

    public static void deleteFileIfExists(String absolutePath) {
        clearCache(absolutePath);
    }

    public static Image loadFxImage(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) return null;
        String src = absolutePath.trim();
        Image cached = getCached(src);
        if (cached != null) return cached;
        if (isKnownMiss(src)) return null;
        try {
            Image img;
            if (isRemotePath(src)) {
                img = new Image(src, true);
            } else {
                Path local = normalizeLocalPath(src);
                if (!Files.isRegularFile(local)) {
                    rememberMiss(src);
                    return null;
                }
                img = new Image(local.toUri().toString(), true);
            }
            if (img.isError()) {
                rememberMiss(src);
                return null;
            }
            putCache(src, img);
            return img;
        } catch (Exception e) {
            rememberMiss(src);
            return null;
        }
    }

    public static String migrateStoredPath(String folderName, String prefix, int entityId, String currentPath) {
        if (currentPath == null || currentPath.isBlank()) return currentPath;
        String src = currentPath.trim();
        if (isRemotePath(src)) return src;
        try {
            Path local = normalizeLocalPath(src);
            if (!Files.isRegularFile(local)) return currentPath;
            return uploadToCloudinary(folderName, prefix, entityId, local);
        } catch (Exception e) {
            return currentPath;
        }
    }

    private static String uploadToCloudinary(String folderName, String prefix, int entityId, Path source) throws IOException {
        String cloudName = firstNonBlank(
            System.getenv("YEBU_CLOUDINARY_CLOUD_NAME"),
            System.getenv("CLOUDINARY_CLOUD_NAME"),
            readConfig("image.cloudinary.cloud_name")
        );
        String uploadPreset = firstNonBlank(
            System.getenv("YEBU_CLOUDINARY_UPLOAD_PRESET"),
            System.getenv("CLOUDINARY_UPLOAD_PRESET"),
            readConfig("image.cloudinary.upload_preset")
        );
        if (cloudName == null || uploadPreset == null) {
            throw new IOException(
                "Cloud image upload is not configured. Set env vars "
                    + "(YEBU_CLOUDINARY_CLOUD_NAME + YEBU_CLOUDINARY_UPLOAD_PRESET) "
                    + "or fill config/db.properties "
                    + "(image.cloudinary.cloud_name + image.cloudinary.upload_preset)."
            );
        }

        String endpoint = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";
        String boundary = "----HotelBoundary" + System.nanoTime();
        String publicId = folderName + "/" + prefix + "-" + entityId + "-" + System.nanoTime();
        String mime = guessMimeType(source.getFileName() == null ? null : source.getFileName().toString());
        byte[] fileBytes = Files.readAllBytes(source);
        byte[] body = buildMultipartBody(boundary, uploadPreset, folderName, publicId, source.getFileName().toString(), mime, fileBytes);

        HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(45))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
        try {
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new IOException("Cloud upload failed (HTTP " + res.statusCode() + ").");
            }
            String secureUrl = extractJsonString(res.body(), "secure_url");
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new IOException("Cloud upload response missing secure_url.");
            }
            clearCache(secureUrl);
            return secureUrl;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Cloud upload interrupted.", e);
        }
    }

    private static byte[] buildMultipartBody(String boundary, String uploadPreset, String folder, String publicId,
                                             String fileName, String mimeType, byte[] fileBytes) {
        String pre = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n" + uploadPreset + "\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"folder\"\r\n\r\n" + folder + "\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"public_id\"\r\n\r\n" + publicId + "\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
            + "Content-Type: " + mimeType + "\r\n\r\n";
        String post = "\r\n--" + boundary + "--\r\n";
        byte[] preBytes = pre.getBytes(StandardCharsets.UTF_8);
        byte[] postBytes = post.getBytes(StandardCharsets.UTF_8);
        byte[] merged = new byte[preBytes.length + fileBytes.length + postBytes.length];
        System.arraycopy(preBytes, 0, merged, 0, preBytes.length);
        System.arraycopy(fileBytes, 0, merged, preBytes.length, fileBytes.length);
        System.arraycopy(postBytes, 0, merged, preBytes.length + fileBytes.length, postBytes.length);
        return merged;
    }

    private static String readConfig(String key) {
        try {
            Path config = findProjectConfig();
            if (!Files.isRegularFile(config)) return null;
            java.util.Properties p = new java.util.Properties();
            try (var in = Files.newInputStream(config)) {
                p.load(in);
            }
            String v = p.getProperty(key);
            return (v == null || v.isBlank()) ? null : v.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static Path findProjectConfig() {
        Path dir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int i = 0; i < MAX_PARENT_WALK; i++) {
            Path candidate = dir.resolve("config").resolve("db.properties");
            if (Files.isRegularFile(candidate)) return candidate;
            Path parent = dir.getParent();
            if (parent == null) break;
            dir = parent;
        }
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize().resolve("config").resolve("db.properties");
    }

    private static String firstNonBlank(String... values) {
        if (values == null || values.length == 0) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    private static String extractJsonString(String json, String key) {
        if (json == null || json.isBlank()) return null;
        String token = "\"" + key + "\":";
        int idx = json.indexOf(token);
        if (idx < 0) return null;
        int firstQuote = json.indexOf('"', idx + token.length());
        if (firstQuote < 0) return null;
        int secondQuote = json.indexOf('"', firstQuote + 1);
        while (secondQuote > 0 && json.charAt(secondQuote - 1) == '\\') {
            secondQuote = json.indexOf('"', secondQuote + 1);
        }
        if (secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote).replace("\\/", "/");
    }

    private static boolean isRemotePath(String path) {
        String p = path.toLowerCase();
        return p.startsWith("http://") || p.startsWith("https://");
    }

    private static Path normalizeLocalPath(String path) {
        String p = path.trim();
        if (p.toLowerCase().startsWith("file:")) {
            return Path.of(URI.create(p));
        }
        return Path.of(p);
    }

    private static String guessMimeType(String filename) {
        if (filename == null) return "application/octet-stream";
        String f = filename.toLowerCase();
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".gif")) return "image/gif";
        if (f.endsWith(".webp")) return "image/webp";
        if (f.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg";
    }

    private static Map<String, Image> createImageCache() {
        return java.util.Collections.synchronizedMap(new LinkedHashMap<>(IMAGE_CACHE_MAX, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                return size() > IMAGE_CACHE_MAX;
            }
        });
    }

    private static Map<String, Instant> createMissCache() {
        return java.util.Collections.synchronizedMap(new LinkedHashMap<>(IMAGE_CACHE_MAX, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Instant> eldest) {
                return size() > IMAGE_CACHE_MAX;
            }
        });
    }

    private static Image getCached(String key) {
        if (key == null || key.isBlank()) return null;
        return IMAGE_CACHE.get(key);
    }

    private static void putCache(String key, Image image) {
        if (key == null || key.isBlank() || image == null) return;
        IMAGE_CACHE.put(key, image);
        MISS_CACHE.remove(key);
    }

    private static boolean isKnownMiss(String key) {
        if (key == null || key.isBlank()) return false;
        Instant at = MISS_CACHE.get(key);
        if (at == null) return false;
        if (Instant.now().isBefore(at.plus(MISS_CACHE_TTL))) return true;
        MISS_CACHE.remove(key);
        return false;
    }

    private static void rememberMiss(String key) {
        if (key == null || key.isBlank()) return;
        MISS_CACHE.put(key, Instant.now());
    }

    private static void clearCache(String key) {
        if (key == null || key.isBlank()) return;
        IMAGE_CACHE.remove(key);
        MISS_CACHE.remove(key);
    }
}

