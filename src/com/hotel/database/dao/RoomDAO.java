package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.Room;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomDAO implements IRoomDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public int save(Room room) {
        String sql = """
            INSERT INTO rooms (room_number, room_type, price_per_night, capacity, description, available, image_path, hotel_id)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType().name());
            ps.setDouble(3, room.getPricePerNight());
            ps.setInt(4, room.getCapacity());
            ps.setString(5, room.getDescription());
            ps.setInt(6, room.isAvailable() ? 1 : 0);
            ps.setString(7, room.getImagePath());
            if (room.getHotelId() == null) ps.setNull(8, java.sql.Types.INTEGER); else ps.setInt(8, room.getHotelId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    room.setId(keys.getInt(1));
                    return room.getId();
                }
            }
            return -1;
        } catch (Exception e) {
            throw new IllegalStateException("save room failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Room room) {
        String sql = """
            UPDATE rooms SET room_number=?, room_type=?, price_per_night=?, capacity=?, description=?, available=?, image_path=?, hotel_id=?
            WHERE id=?
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType().name());
            ps.setDouble(3, room.getPricePerNight());
            ps.setInt(4, room.getCapacity());
            ps.setString(5, room.getDescription());
            ps.setInt(6, room.isAvailable() ? 1 : 0);
            ps.setString(7, room.getImagePath());
            if (room.getHotelId() == null) ps.setNull(8, java.sql.Types.INTEGER); else ps.setInt(8, room.getHotelId());
            ps.setInt(9, room.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("update room failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int roomId) {
        if (hasActiveReservation(roomId)) return false;
        String sql = "DELETE FROM rooms WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("delete room failed: " + e.getMessage(), e);
        }
    }

    private boolean hasActiveReservation(int roomId) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_id=? AND status <> 'CANCELLED'
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public List<Room> findAll() {
        List<Room> list = new ArrayList<>();
        String sql = """
            SELECT r.id, r.room_number, r.room_type, r.price_per_night, r.capacity, r.description, r.available, r.image_path, r.hotel_id,
                   h.name AS hotel_name
            FROM rooms r
            LEFT JOIN hotels h ON r.hotel_id = h.id
            ORDER BY r.id
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRoom(rs));
        } catch (Exception e) {
            throw new IllegalStateException("findAll rooms failed: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Room> findByHotelId(int hotelId) {
        String sql = """
            SELECT r.id, r.room_number, r.room_type, r.price_per_night, r.capacity, r.description, r.available, r.image_path, r.hotel_id,
                   h.name AS hotel_name
            FROM rooms r
            LEFT JOIN hotels h ON r.hotel_id = h.id
            WHERE r.hotel_id=?
            ORDER BY r.id
            """;
        List<Room> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, hotelId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRoom(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("findByHotelId rooms failed: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<Room> findById(int roomId) {
        String sql = """
            SELECT r.id, r.room_number, r.room_type, r.price_per_night, r.capacity, r.description, r.available, r.image_path, r.hotel_id,
                   h.name AS hotel_name
            FROM rooms r
            LEFT JOIN hotels h ON r.hotel_id = h.id
            WHERE r.id=?
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRoom(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("findById room failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, String roomType) {
        return findAvailableRooms(checkIn, checkOut, roomType, null);
    }

    @Override
    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, String roomType, Integer hotelId) {
        String typeFilter = roomType == null ? "ALL" : roomType;
        String sql = """
            SELECT r.id, r.room_number, r.room_type, r.price_per_night, r.capacity, r.description, r.available, r.image_path, r.hotel_id,
                   h.name AS hotel_name
            FROM rooms r
            LEFT JOIN hotels h ON r.hotel_id = h.id
            WHERE r.available = 1
            AND (? = 'ALL' OR r.room_type = ?)
            AND (? IS NULL OR r.hotel_id = ?)
            AND NOT EXISTS (
              SELECT 1 FROM reservations res
              WHERE res.room_id = r.id
              AND res.status <> 'CANCELLED'
              AND res.check_in < ?
              AND res.check_out > ?
            )
            ORDER BY r.id
            """;
        List<Room> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, typeFilter);
            ps.setString(2, typeFilter);
            ps.setObject(3, hotelId);
            ps.setObject(4, hotelId);
            ps.setObject(5, checkOut);
            ps.setObject(6, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRoom(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("findAvailableRooms failed: " + e.getMessage(), e);
        }
        return list;
    }

    static Room mapRoom(ResultSet rs) throws Exception {
        Room room = new Room();
        room.setId(rs.getInt("id"));
        room.setRoomNumber(rs.getString("room_number"));
        room.setRoomType(Room.RoomType.valueOf(rs.getString("room_type")));
        room.setPricePerNight(rs.getDouble("price_per_night"));
        room.setCapacity(rs.getInt("capacity"));
        room.setDescription(rs.getString("description"));
        room.setAvailable(rs.getInt("available") == 1);
        room.setImagePath(rs.getString("image_path"));
        int hotelId = rs.getInt("hotel_id");
        room.setHotelId(rs.wasNull() ? null : hotelId);
        try {
            room.setHotelName(rs.getString("hotel_name"));
        } catch (Exception ignored) {
            room.setHotelName(null);
        }
        return room;
    }
}
