package com.hotel.database;

import com.hotel.model.Room;
import com.hotel.model.User;
import com.hotel.database.dao.RoomDAO;
import com.hotel.database.dao.UserDAO;
import com.hotel.service.ExtraServiceCatalogService;
import com.hotel.util.ImageStorageService;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class DatabaseInitializer {
    private static boolean initialized = false;

    private DatabaseInitializer() {}

    public static synchronized void initialize() {
        if (initialized) return;
        DatabaseConnection db = DatabaseConnection.getInstance();
        db.ensureSchema();
        cleanupOrphanRooms(db);
        runImageMigrationOnce(db);
        new ExtraServiceCatalogService().ensureDefaultServices();
        if (tableIsEmpty(db, "users")) {
            User admin = new User(
                "System Admin",
                "admin@hotel.com",
                BCrypt.hashpw("admin123", BCrypt.gensalt(10)),
                "0000000000",
                "ADMIN"
            );
            new UserDAO().save(admin);
            RoomDAO roomDAO = new RoomDAO();
            addRoom(roomDAO, "101", Room.RoomType.SINGLE, 1400, 1, "Budget single room");
            addRoom(roomDAO, "102", Room.RoomType.DOUBLE, 2100, 2, "Comfort double room");
            addRoom(roomDAO, "201", Room.RoomType.SUITE, 3400, 3, "Sea view suite");
            addRoom(roomDAO, "301", Room.RoomType.DELUXE, 4200, 4, "Deluxe family room");
        }
        initialized = true;
    }

    private static void runImageMigrationOnce(DatabaseConnection db) {
        boolean migrationDone = isMetaTrue(db, "image_migration_v2_done");
        if (migrationDone && !hasLegacyImagePaths(db)) return;
        migrateImagePathsToSharedMedia(db);
        setMetaValue(db, "image_migration_v2_done", "true");
    }

    private static void cleanupOrphanRooms(DatabaseConnection db) {
        String sql = """
            DELETE FROM rooms
            WHERE hotel_id IS NULL
               OR NOT EXISTS (SELECT 1 FROM hotels h WHERE h.id = rooms.hotel_id)
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Orphan room cleanup failed: " + e.getMessage());
        }
    }

    private static boolean tableIsEmpty(DatabaseConnection db, String table) {
        String sql = "SELECT COUNT(*) FROM " + table;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getLong(1) == 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check table " + table + ": " + e.getMessage(), e);
        }
    }

    private static void addRoom(RoomDAO roomDAO, String no, Room.RoomType type, double price, int capacity, String desc) {
        Room room = new Room(no, type, price, capacity, desc);
        room.setAvailable(true);
        roomDAO.save(room);
    }

    private static void migrateImagePathsToSharedMedia(DatabaseConnection db) {
        migrateTableImagePaths(db, "hotels", "id", "image_path", "hotel-images", "hotel");
        migrateTableImagePaths(db, "rooms", "id", "image_path", "room-images", "room");
        migrateTableImagePaths(db, "room_images", "id", "image_path", "room-images", "room");
    }

    private static void migrateTableImagePaths(DatabaseConnection db, String tableName, String idCol, String imageCol,
                                               String folderName, String prefix) {
        String selectSql = "SELECT " + idCol + ", " + imageCol + " FROM " + tableName + " WHERE " + imageCol + " IS NOT NULL AND " + imageCol + " <> ''";
        String updateSql = "UPDATE " + tableName + " SET " + imageCol + " = ? WHERE " + idCol + " = ?";
        try (Connection c = db.getConnection();
             PreparedStatement psSelect = c.prepareStatement(selectSql);
             PreparedStatement psUpdate = c.prepareStatement(updateSql);
             ResultSet rs = psSelect.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt(idCol);
                String currentPath = rs.getString(imageCol);
                String migrated = ImageStorageService.migrateStoredPath(folderName, prefix, id, currentPath);
                if (migrated != null && !migrated.equals(currentPath)) {
                    psUpdate.setString(1, migrated);
                    psUpdate.setInt(2, id);
                    psUpdate.addBatch();
                }
            }
            psUpdate.executeBatch();
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Image path migration failed for " + tableName + ": " + e.getMessage());
        }
    }

    private static boolean isMetaTrue(DatabaseConnection db, String key) {
        String sql = "SELECT meta_value FROM app_meta WHERE meta_key = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String value = rs.getString(1);
                return value != null && "true".equalsIgnoreCase(value.trim());
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static void setMetaValue(DatabaseConnection db, String key, String value) {
        String sql = """
            INSERT INTO app_meta (meta_key, meta_value)
            VALUES (?,?)
            ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value)
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] app_meta update failed: " + e.getMessage());
        }
    }

    private static boolean hasLegacyImagePaths(DatabaseConnection db) {
        return tableHasLegacyPaths(db, "hotels", "image_path")
            || tableHasLegacyPaths(db, "rooms", "image_path")
            || tableHasLegacyPaths(db, "room_images", "image_path");
    }

    private static boolean tableHasLegacyPaths(DatabaseConnection db, String table, String col) {
        String sql = String.format(
            "SELECT COUNT(*) FROM %s " +
                "WHERE %s IS NOT NULL " +
                "AND %s <> '' " +
                "AND %s NOT LIKE 'shared-media/%%' " +
                "AND LOWER(%s) NOT LIKE 'http://%%' " +
                "AND LOWER(%s) NOT LIKE 'https://%%'",
            table, col, col, col, col, col
        );
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getLong(1) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
