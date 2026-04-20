package com.hotel.util;

import com.hotel.database.dao.MediaAssetDAO;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ImageStorageService {
    private static final String SHARED_MEDIA_ROOT = "shared-media";
    private static final MediaAssetDAO MEDIA_ASSET_DAO = new MediaAssetDAO();
    private static final int IMAGE_CACHE_MAX = 400;
    private static final Map<String, Image> IMAGE_CACHE = createImageCache();
    private static final Map<String, Boolean> MISS_CACHE = createMissCache();

    private ImageStorageService() {}

    public static String copyToEntityFile(String folderName, String prefix, int entityId, File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.isFile()) {
            throw new IllegalArgumentException("Select a valid image file.");
        }
        String ext = extensionOf(sourceFile.getName());
        String fileName = prefix + "-" + entityId + "-" + System.nanoTime() + ext;

        // Prefer relative shared path in DB so it works across machines.
        String pathKey = SHARED_MEDIA_ROOT + "/" + folderName + "/" + fileName;
        persistMediaAsset(pathKey, sourceFile.toPath());
        return pathKey;
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

    public static Path getSharedStorageDir(String folderName) {
        Path dir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
            .resolve(SHARED_MEDIA_ROOT)
            .resolve(folderName);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create shared image directory: " + dir, e);
        }
        return dir;
    }

    public static void deleteFileIfExists(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) return;
        String src = absolutePath.trim();
        if (isRemotePath(src)) return;
        try {
            Path normalized = normalizeLocalPath(src);
            String filename = normalized.getFileName() != null ? normalized.getFileName().toString() : null;
            String key = resolvePathKey(src, filename);
            if (key != null) {
                MEDIA_ASSET_DAO.deleteByPathKey(key);
                clearCache(src);
                clearCache(key);
            }
        } catch (Exception ignored) {
        }
    }

    public static Image loadFxImage(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) return null;
        try {
            String src = absolutePath.trim();
            Image cached = getCached(src);
            if (cached != null) return cached;
            if (isKnownMiss(src)) return null;
            if (isRemotePath(src)) {
                Image remote = new Image(src, true);
                if (remote.isError()) {
                    rememberMiss(src);
                    return null;
                }
                putCache(src, remote);
                return remote;
            }

            Path local = normalizeLocalPath(src);
            String filename = local.getFileName() != null ? local.getFileName().toString() : null;
            String key = resolvePathKey(src, filename);
            if (key != null) {
                Image keyCached = getCached(key);
                if (keyCached != null) {
                    putCache(src, keyCached);
                    return keyCached;
                }
            }
            Image fromAsset = loadImageFromAsset(key, filename);
            if (fromAsset != null) {
                putCache(src, fromAsset);
                if (key != null) putCache(key, fromAsset);
                return fromAsset;
            }

            // Legacy compatibility only: if DB blob is missing but path exists locally.
            Path existingPath = resolveExistingLocalPath(src);
            if (existingPath != null) {
                Image img = new Image(existingPath.toUri().toString(), true);
                if (img.isError()) {
                    rememberMiss(src);
                    return null;
                }
                putCache(src, img);
                return img;
            }
            rememberMiss(src);
            return null;
        } catch (Exception e) {
            rememberMiss(absolutePath);
            return null;
        }
    }

    public static String migrateStoredPath(String folderName, String prefix, int entityId, String currentPath) {
        if (currentPath == null || currentPath.isBlank()) return currentPath;
        String src = currentPath.trim().replace('\\', '/');
        if (isRemotePath(src)) return src;

        String sharedPrefix = SHARED_MEDIA_ROOT + "/" + folderName + "/";
        if (src.startsWith(sharedPrefix)) return src;

        String marker = "/" + SHARED_MEDIA_ROOT + "/" + folderName + "/";
        int markerIndex = src.indexOf(marker);
        if (markerIndex >= 0) {
            String filePart = src.substring(markerIndex + marker.length());
            return sharedPrefix + filePart;
        }

        try {
            Path existing = resolveExistingLocalPath(src);
            if (existing == null) {
                Path normalized = normalizeLocalPath(src);
                String filename = normalized.getFileName() != null ? normalized.getFileName().toString() : null;
                existing = resolveFallbackByFilename(filename);
            }
            if (existing == null || !Files.isRegularFile(existing)) return currentPath;

            String ext = extensionOf(existing.getFileName().toString());
            String fileName = prefix + "-" + entityId + "-migrated-" + System.nanoTime() + ext;
            String pathKey = sharedPrefix + fileName;
            persistMediaAsset(pathKey, existing);
            return pathKey;
        } catch (Exception e) {
            return currentPath;
        }
    }

    private static Path resolveFallbackByFilename(String filename) {
        if (filename == null || filename.isBlank()) return null;
        Path[] bases = new Path[] {
            getStorageDir("room-images"),
            getStorageDir("hotel-images"),
            getSharedStorageDir("room-images"),
            getSharedStorageDir("hotel-images")
        };
        for (Path base : bases) {
            Path candidate = base.resolve(filename);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        return null;
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

    private static Path resolveExistingLocalPath(String src) {
        Path direct = normalizeLocalPath(src);
        if (Files.isRegularFile(direct)) return direct;

        // If value is relative, try under project directory explicitly.
        if (!direct.isAbsolute()) {
            Path projectRelative = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize().resolve(src);
            if (Files.isRegularFile(projectRelative)) return projectRelative;
        }
        return null;
    }

    private static void persistMediaAsset(String pathKey, Path source) {
        try {
            byte[] data = Files.readAllBytes(source);
            String mime = guessMimeType(source.getFileName() != null ? source.getFileName().toString() : null);
            MEDIA_ASSET_DAO.upsert(pathKey, mime, data);
        } catch (Exception ignored) {
            // Do not fail upload because blob persistence failed.
        }
    }

    private static String resolvePathKey(String src, String filename) {
        String normalized = src.replace('\\', '/');
        if (normalized.startsWith(SHARED_MEDIA_ROOT + "/")) return normalized;
        String marker = "/" + SHARED_MEDIA_ROOT + "/";
        int idx = normalized.indexOf(marker);
        if (idx >= 0) return normalized.substring(idx + 1);
        var byName = MEDIA_ASSET_DAO.findLatestByFilename(filename);
        return byName.map(MediaAssetDAO.MediaAsset::pathKey).orElse(null);
    }

    private static Image loadImageFromAsset(String pathKey, String filename) {
        if ((pathKey == null || pathKey.isBlank()) && (filename == null || filename.isBlank())) return null;
        var assetOpt = pathKey != null && !pathKey.isBlank()
            ? MEDIA_ASSET_DAO.findByPathKey(pathKey)
            : java.util.Optional.<MediaAssetDAO.MediaAsset>empty();
        if (assetOpt.isEmpty() && filename != null) assetOpt = MEDIA_ASSET_DAO.findLatestByFilename(filename);
        if (assetOpt.isEmpty()) return null;
        try {
            var asset = assetOpt.get();
            Image img = new Image(new ByteArrayInputStream(asset.data()));
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
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

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return ".jpg";
        String ext = filename.substring(dot).toLowerCase();
        if (!ext.matches("\\.(jpg|jpeg|png|gif|bmp|webp)")) return ".jpg";
        return ext;
    }

    private static Map<String, Image> createImageCache() {
        return java.util.Collections.synchronizedMap(new LinkedHashMap<>(IMAGE_CACHE_MAX, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                return size() > IMAGE_CACHE_MAX;
            }
        });
    }

    private static Map<String, Boolean> createMissCache() {
        return java.util.Collections.synchronizedMap(new LinkedHashMap<>(IMAGE_CACHE_MAX, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
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
        return MISS_CACHE.containsKey(key);
    }

    private static void rememberMiss(String key) {
        if (key == null || key.isBlank()) return;
        MISS_CACHE.put(key, Boolean.TRUE);
    }

    private static void clearCache(String key) {
        if (key == null || key.isBlank()) return;
        IMAGE_CACHE.remove(key);
        MISS_CACHE.remove(key);
    }
}

