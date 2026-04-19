package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.RoomImage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomImageDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<RoomImage> findByRoomId(int roomId) {
        String sql = """
            SELECT id, room_id, image_path, sort_order
            FROM room_images
            WHERE room_id=?
            ORDER BY sort_order ASC, id ASC
            """;
        List<RoomImage> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoomImage ri = new RoomImage();
                    ri.setId(rs.getInt("id"));
                    ri.setRoomId(rs.getInt("room_id"));
                    ri.setImagePath(rs.getString("image_path"));
                    ri.setSortOrder(rs.getInt("sort_order"));
                    list.add(ri);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("find room_images failed: " + e.getMessage(), e);
        }
        return list;
    }

    public int add(RoomImage image) {
        String sql = """
            INSERT INTO room_images (room_id, image_path, sort_order)
            VALUES (?,?,?)
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, image.getRoomId());
            ps.setString(2, image.getImagePath());
            ps.setInt(3, image.getSortOrder());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    image.setId(id);
                    return id;
                }
            }
            return -1;
        } catch (Exception e) {
            throw new IllegalStateException("add room_image failed: " + e.getMessage(), e);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM room_images WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("delete room_image failed: " + e.getMessage(), e);
        }
    }

    public boolean deleteByRoomId(int roomId) {
        String sql = "DELETE FROM room_images WHERE room_id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("delete room_images by room failed: " + e.getMessage(), e);
        }
    }

    public int getNextSortOrder(int roomId) {
        String sql = "SELECT COALESCE(MAX(sort_order), -1) + 1 FROM room_images WHERE room_id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("getNextSortOrder failed: " + e.getMessage(), e);
        }
    }
}

