package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.Room;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
                   h.name AS hotel_name, h.image_path AS hotel_image_path
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
                   h.name AS hotel_name, h.image_path AS hotel_image_path
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
                   h.name AS hotel_name, h.image_path AS hotel_image_path
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
                   h.name AS hotel_name, h.image_path AS hotel_image_path
            FROM rooms r
            JOIN hotels h ON r.hotel_id = h.id
            WHERE r.available = 1
            AND (? = 'ALL' OR r.room_type = ?)
            AND (? IS NULL OR r.hotel_id = ?)
            AND (
              NOT EXISTS (SELECT 1 FROM room_availability ra0 WHERE ra0.room_id = r.id)
              OR (
                SELECT COUNT(*) FROM room_availability ra
                WHERE ra.room_id = r.id
                  AND ra.available_date >= ?
                  AND ra.available_date < ?
              ) >= DATEDIFF(?, ?)
            )
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
            ps.setObject(5, checkIn);
            ps.setObject(6, checkOut);
            ps.setObject(7, checkOut);
            ps.setObject(8, checkIn);
            ps.setObject(9, checkOut);
            ps.setObject(10, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRoom(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("findAvailableRooms failed: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void replaceAvailabilityRange(int roomId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Availability range is invalid.");
        }
        String deleteSql = "DELETE FROM room_availability WHERE room_id = ?";
        String insertSql = "INSERT INTO room_availability (room_id, available_date) VALUES (?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement del = c.prepareStatement(deleteSql);
             PreparedStatement ins = c.prepareStatement(insertSql)) {
            boolean oldAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                del.setInt(1, roomId);
                del.executeUpdate();

                LocalDate day = startDate;
                while (day.isBefore(endDate)) {
                    ins.setInt(1, roomId);
                    ins.setObject(2, day);
                    ins.addBatch();
                    day = day.plusDays(1);
                }
                ins.executeBatch();
                c.commit();
                c.setAutoCommit(oldAutoCommit);
            } catch (Exception inner) {
                c.rollback();
                c.setAutoCommit(oldAutoCommit);
                throw inner;
            }
        } catch (Exception e) {
            throw new IllegalStateException("replace availability failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Set<LocalDate> getAvailableDates(int roomId) {
        String sql = """
            SELECT available_date
            FROM room_availability
            WHERE room_id = ?
            ORDER BY available_date
            """;
        Set<LocalDate> dates = new HashSet<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dates.add(rs.getObject("available_date", LocalDate.class));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("get available dates failed: " + e.getMessage(), e);
        }
        return dates;
    }

    @Override
    public boolean isAvailableForRange(int roomId, LocalDate checkIn, LocalDate checkOut) {
        String anySql = "SELECT COUNT(*) FROM room_availability WHERE room_id = ?";
        String sql = """
            SELECT COUNT(*) AS free_days
            FROM room_availability
            WHERE room_id = ?
              AND available_date >= ?
              AND available_date < ?
            """;
        try (Connection c = db.getConnection();
             PreparedStatement anyPs = c.prepareStatement(anySql);
             PreparedStatement ps = c.prepareStatement(sql)) {
            anyPs.setInt(1, roomId);
            try (ResultSet anyRs = anyPs.executeQuery()) {
                if (anyRs.next() && anyRs.getInt(1) == 0) return true;
            }
            ps.setInt(1, roomId);
            ps.setObject(2, checkIn);
            ps.setObject(3, checkOut);
            try (ResultSet rs = ps.executeQuery()) {
                int expectedDays = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
                if (!rs.next()) return false;
                return rs.getInt("free_days") >= expectedDays;
            }
        } catch (Exception e) {
            throw new IllegalStateException("isAvailableForRange failed: " + e.getMessage(), e);
        }
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
            room.setHotelImagePath(rs.getString("hotel_image_path"));
        } catch (Exception ignored) {
            room.setHotelName(null);
            room.setHotelImagePath(null);
        }
        return room;
    }
}
