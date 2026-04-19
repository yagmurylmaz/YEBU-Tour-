package com.hotel.database;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class DbConfig {

    private static final int MAX_PARENT_WALK = 12;

    private DbConfig() {}

    static Properties load() {
        Properties merged = new Properties();
        try (InputStream defaults = DbConfig.class.getResourceAsStream("/database.properties")) {
            if (defaults != null) merged.load(defaults);
        } catch (Exception e) {
            throw new IllegalStateException("Missing classpath /database.properties", e);
        }
        Path local = findProjectConfig();
        if (local != null && Files.exists(local)) {
            try (var in = Files.newInputStream(local)) {
                Properties localProps = new Properties();
                localProps.load(in);
                merged.putAll(localProps);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read " + local + ": " + e.getMessage(), e);
            }
        }
        String envPw = System.getenv("YEBU_DB_PASSWORD");
        if (envPw != null && !envPw.isBlank()) {
            merged.setProperty("db.password", envPw);
        }
        require(merged, "db.url");
        require(merged, "db.user");
        require(merged, "db.password");
        return merged;
    }

    /**
     * Resolves {@code config/db.properties} relative to the JVM working directory, then walks
     * parent directories so the app still finds the file when launched from a subfolder or IDE
     * with a non-project cwd.
     */
    private static Path findProjectConfig() {
        Path start = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path dir = start;
        for (int i = 0; i < MAX_PARENT_WALK; i++) {
            Path candidate = dir.resolve("config").resolve("db.properties");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            Path parent = dir.getParent();
            if (parent == null) break;
            dir = parent;
        }
        return null;
    }

    private static void require(Properties p, String key) {
        if (p.getProperty(key) == null || p.getProperty(key).isBlank()) {
            if ("db.password".equals(key)) return;
            throw new IllegalStateException("Missing property: " + key);
        }
    }
}
