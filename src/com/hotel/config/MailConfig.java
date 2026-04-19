package com.hotel.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class MailConfig {

    private static final int MAX_PARENT_WALK = 20;

    private MailConfig() {}

    public static Properties load() {
        Properties merged = new Properties();
        try (InputStream defaults = MailConfig.class.getResourceAsStream("/mail.properties")) {
            if (defaults != null) {
                try (InputStreamReader r = new InputStreamReader(defaults, StandardCharsets.UTF_8)) {
                    merged.load(r);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load /mail.properties from classpath", e);
        }

        try (InputStream ctx = Thread.currentThread().getContextClassLoader().getResourceAsStream("mail.properties")) {
            if (ctx != null) {
                try (InputStreamReader r = new InputStreamReader(ctx, StandardCharsets.UTF_8)) {
                    Properties extra = new Properties();
                    extra.load(r);
                    merged.putAll(extra);
                }
            }
        } catch (Exception ignored) {
            // optional second classpath lookup
        }

        Path explicit = explicitMailPropertiesPath();
        if (explicit != null) {
            loadUtf8PropertiesFile(explicit, merged);
        }

        // IDE runs often omit `resources/` from the classpath; merge from disk so edits apply.
        mergeFirstExisting(merged, findResourcesMailPropertiesFromUserDir());
        mergeFirstExisting(merged, findResourcesMailPropertiesNearCodeSource());
        mergeFirstExisting(merged, findProjectConfig());
        normalizePassword(merged);
        applyEnvOverrides(merged);
        normalizePassword(merged);
        return merged;
    }

    private static void mergeFirstExisting(Properties into, Path path) {
        if (path != null && Files.isRegularFile(path)) {
            loadUtf8PropertiesFile(path, into);
        }
    }

    /** -Dyebu.mail.config=/path or env YEBU_MAIL_PROPERTIES_FILE */
    private static Path explicitMailPropertiesPath() {
        String p = System.getProperty("yebu.mail.config");
        if (p == null || p.isBlank()) {
            p = System.getenv("YEBU_MAIL_PROPERTIES_FILE");
        }
        if (p == null || p.isBlank()) {
            return null;
        }
        Path path = Path.of(p.trim()).toAbsolutePath().normalize();
        return Files.isRegularFile(path) ? path : null;
    }

    /**
     * When {@code user.dir} is not the project root (e.g. IDE uses another working directory),
     * walking from the compiled class location finds {@code resources/mail.properties}.
     */
    private static Path findResourcesMailPropertiesNearCodeSource() {
        try {
            var source = MailConfig.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return null;
            }
            URI uri = source.getLocation().toURI();
            Path start = Path.of(uri).toAbsolutePath().normalize();
            if (Files.isRegularFile(start) && start.toString().toLowerCase().endsWith(".jar")) {
                start = start.getParent();
            }
            Path dir = start;
            for (int i = 0; i < MAX_PARENT_WALK; i++) {
                Path candidate = dir.resolve("resources").resolve("mail.properties");
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
                Path parent = dir.getParent();
                if (parent == null) {
                    break;
                }
                dir = parent;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private static void loadUtf8PropertiesFile(Path path, Properties into) {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Properties chunk = new Properties();
            chunk.load(reader);
            into.putAll(chunk);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + path + ": " + e.getMessage(), e);
        }
    }

    /** Gmail app passwords are often shown with spaces; SMTP expects the 16-character form without spaces. */
    private static void normalizePassword(Properties p) {
        String pw = p.getProperty("mail.password");
        if (pw != null && !pw.isBlank()) {
            p.setProperty("mail.password", pw.replaceAll("\\s+", ""));
        }
    }

    private static Path findResourcesMailPropertiesFromUserDir() {
        Path start = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path dir = start;
        for (int i = 0; i < MAX_PARENT_WALK; i++) {
            Path candidate = dir.resolve("resources").resolve("mail.properties");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            Path parent = dir.getParent();
            if (parent == null) break;
            dir = parent;
        }
        return null;
    }

    /**
     * Optional env vars (override file-based config): YEBU_SMTP_HOST, YEBU_SMTP_PORT, YEBU_MAIL_USER,
     * YEBU_MAIL_FROM, YEBU_MAIL_PASSWORD, YEBU_SMTP_AUTH (true/false).
     */
    private static void applyEnvOverrides(Properties merged) {
        putEnv(merged, "YEBU_SMTP_HOST", "mail.smtp.host");
        putEnv(merged, "YEBU_SMTP_PORT", "mail.smtp.port");
        putEnv(merged, "YEBU_MAIL_USER", "mail.user");
        putEnv(merged, "YEBU_MAIL_FROM", "mail.from");
        String envPw = System.getenv("YEBU_MAIL_PASSWORD");
        if (envPw != null && !envPw.isBlank()) {
            merged.setProperty("mail.password", envPw.trim());
        }
        String authEnv = System.getenv("YEBU_SMTP_AUTH");
        if (authEnv != null && !authEnv.isBlank()) {
            merged.setProperty("mail.smtp.auth", Boolean.parseBoolean(authEnv.trim()) ? "true" : "false");
        }
    }

    private static void putEnv(Properties p, String envKey, String propKey) {
        String v = System.getenv(envKey);
        if (v != null && !v.isBlank()) {
            p.setProperty(propKey, v.trim());
        }
    }

    public static boolean isSmtpConfigured(Properties p) {
        String host = p.getProperty("mail.smtp.host", "").trim();
        String from = p.getProperty("mail.from", "").trim();
        boolean auth = Boolean.parseBoolean(p.getProperty("mail.smtp.auth", "true"));
        String user = p.getProperty("mail.user", "").trim();
        String password = p.getProperty("mail.password", "").trim();
        if (host.isEmpty() || from.isEmpty()) return false;
        if (auth && (user.isEmpty() || password.isEmpty())) return false;
        return true;
    }

    /** English hint listing what is still missing (for UI error text). */
    public static String smtpMissingHint(Properties p) {
        boolean auth = Boolean.parseBoolean(p.getProperty("mail.smtp.auth", "true"));
        String host = p.getProperty("mail.smtp.host", "").trim();
        String from = p.getProperty("mail.from", "").trim();
        String user = p.getProperty("mail.user", "").trim();
        String password = p.getProperty("mail.password", "").trim();

        StringBuilder sb = new StringBuilder();
        if (host.isEmpty()) {
            sb.append("mail.smtp.host is empty (e.g. smtp.gmail.com) or set YEBU_SMTP_HOST. ");
        }
        if (from.isEmpty()) {
            sb.append("mail.from is empty (sender address) or set YEBU_MAIL_FROM. ");
        }
        if (auth) {
            if (user.isEmpty()) {
                sb.append("mail.user is empty or set YEBU_MAIL_USER. ");
            }
            if (password.isEmpty()) {
                sb.append("mail.password is empty or set YEBU_MAIL_PASSWORD. ");
            }
        }
        if (sb.isEmpty()) {
            return "";
        }
        return "Missing SMTP settings: " + sb
            + "Edit resources/mail.properties, or config/mail.properties, or set YEBU_* env vars. "
            + "If the IDE uses another working folder, set YEBU_MAIL_PROPERTIES_FILE to the full path of mail.properties.";
    }

    private static Path findProjectConfig() {
        Path start = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path dir = start;
        for (int i = 0; i < MAX_PARENT_WALK; i++) {
            Path candidate = dir.resolve("config").resolve("mail.properties");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            Path parent = dir.getParent();
            if (parent == null) break;
            dir = parent;
        }
        try {
            var source = MailConfig.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                Path p = Path.of(source.getLocation().toURI()).toAbsolutePath().normalize();
                if (Files.isRegularFile(p) && p.toString().toLowerCase().endsWith(".jar")) {
                    p = p.getParent();
                }
                Path walk = p;
                for (int i = 0; i < MAX_PARENT_WALK; i++) {
                    Path candidate = walk.resolve("config").resolve("mail.properties");
                    if (Files.isRegularFile(candidate)) {
                        return candidate;
                    }
                    Path parent = walk.getParent();
                    if (parent == null) break;
                    walk = parent;
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }
}
