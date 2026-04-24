package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class FavoriteHotelDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public Set<Integer> findFavoriteHotelIdsByUser(int userId) {
        String sql = "SELECT hotel_id FROM user_favorite_hotels WHERE user_id = ?";
        Set<Integer> ids = new HashSet<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("hotel_id"));
                }
            }
            return ids;
        } catch (Exception e) {
            throw new IllegalStateException("find favorites failed: " + e.getMessage(), e);
        }
    }

    public boolean addFavorite(int userId, int hotelId) {
        String sql = """
            INSERT INTO user_favorite_hotels (user_id, hotel_id)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hotelId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("add favorite failed: " + e.getMessage(), e);
        }
    }

    public boolean removeFavorite(int userId, int hotelId) {
        String sql = "DELETE FROM user_favorite_hotels WHERE user_id = ? AND hotel_id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, hotelId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("remove favorite failed: " + e.getMessage(), e);
        }
    }
}
