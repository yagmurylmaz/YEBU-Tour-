package com.hotel.database;

import com.hotel.model.Room;
import com.hotel.model.User;
import org.mindrot.jbcrypt.BCrypt;

public final class DatabaseInitializer {
    private static boolean initialized = false;

    private DatabaseInitializer() {}

    public static synchronized void initialize() {
        if (initialized) return;
        DatabaseConnection db = DatabaseConnection.getInstance();
        if (db.hasAnyData()) {
            initialized = true;
            return;
        }

        User admin = new User(
            "System Admin",
            "admin@hotel.com",
            BCrypt.hashpw("admin123", BCrypt.gensalt(10)),
            "0000000000",
            "ADMIN"
        );
        admin.setId(db.nextUserId());
        db.users().add(admin);

        addRoom(db, "101", Room.RoomType.SINGLE, 1400, 1, "Budget single room");
        addRoom(db, "102", Room.RoomType.DOUBLE, 2100, 2, "Comfort double room");
        addRoom(db, "201", Room.RoomType.SUITE, 3400, 3, "Sea view suite");
        addRoom(db, "301", Room.RoomType.DELUXE, 4200, 4, "Deluxe family room");

        db.persistAll();
        initialized = true;
    }

    private static void addRoom(DatabaseConnection db, String no, Room.RoomType type, double price, int capacity, String desc) {
        Room room = new Room(no, type, price, capacity, desc);
        room.setId(db.nextRoomId());
        room.setAvailable(true);
        db.rooms().add(room);
    }
}
