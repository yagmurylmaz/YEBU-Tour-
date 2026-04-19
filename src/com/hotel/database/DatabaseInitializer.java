package com.hotel.database;

import com.hotel.model.Room;
import com.hotel.model.User;
import com.hotel.database.dao.RoomDAO;
import com.hotel.database.dao.UserDAO;
import com.hotel.service.ExtraServiceCatalogService;
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
}
