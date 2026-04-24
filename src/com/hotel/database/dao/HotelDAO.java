package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.Hotel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HotelDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public int add(Hotel hotel) {
        String sql = """
            INSERT INTO hotels (name, country_id, city_id, address_line, phone, email, image_path)
            VALUES (?,?,?,?,?,?,?)
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, hotel.getName());
            ps.setInt(2, hotel.getCountryId());
            ps.setInt(3, hotel.getCityId());
            ps.setString(4, hotel.getAddressLine());
            ps.setString(5, hotel.getPhone());
            ps.setString(6, hotel.getEmail());
            ps.setString(7, hotel.getImagePath());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    hotel.setId(id);
                    return id;
                }
            }
            return -1;
        } catch (Exception e) {
            throw new IllegalStateException("add hotel failed: " + e.getMessage(), e);
        }
    }

    public boolean update(Hotel hotel) {
        String sql = """
            UPDATE hotels SET name=?, country_id=?, city_id=?, address_line=?, phone=?, email=?, image_path=?
            WHERE id=?
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hotel.getName());
            ps.setInt(2, hotel.getCountryId());
            ps.setInt(3, hotel.getCityId());
            ps.setString(4, hotel.getAddressLine());
            ps.setString(5, hotel.getPhone());
            ps.setString(6, hotel.getEmail());
            ps.setString(7, hotel.getImagePath());
            ps.setInt(8, hotel.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("update hotel failed: " + e.getMessage(), e);
        }
    }

    public boolean delete(int hotelId) {
        String deleteRoomsSql = "DELETE FROM rooms WHERE hotel_id=?";
        String deleteHotelSql = "DELETE FROM hotels WHERE id=?";
        try (Connection c = db.getConnection();
             PreparedStatement psRooms = c.prepareStatement(deleteRoomsSql);
             PreparedStatement psHotel = c.prepareStatement(deleteHotelSql)) {
            boolean oldAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                psRooms.setInt(1, hotelId);
                psRooms.executeUpdate();

                psHotel.setInt(1, hotelId);
                boolean deleted = psHotel.executeUpdate() > 0;

                c.commit();
                c.setAutoCommit(oldAutoCommit);
                return deleted;
            } catch (Exception inner) {
                c.rollback();
                c.setAutoCommit(oldAutoCommit);
                throw inner;
            }
        } catch (Exception e) {
            throw new IllegalStateException("delete hotel failed: " + e.getMessage(), e);
        }
    }

    public List<Hotel> findAll() {
        String sql = """
            SELECT h.id, h.name, h.country_id, h.city_id, h.address_line, h.phone, h.email,
                   h.image_path,
                   co.name AS country_name, ci.name AS city_name
            FROM hotels h
            LEFT JOIN countries co ON h.country_id = co.id
            LEFT JOIN cities ci ON h.city_id = ci.id
            ORDER BY h.id
            """;
        List<Hotel> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (Exception e) {
            throw new IllegalStateException("findAll hotels failed: " + e.getMessage(), e);
        }
        return list;
    }

    public Optional<Hotel> findById(int id) {
        String sql = """
            SELECT h.id, h.name, h.country_id, h.city_id, h.address_line, h.phone, h.email,
                   h.image_path,
                   co.name AS country_name, ci.name AS city_name
            FROM hotels h
            LEFT JOIN countries co ON h.country_id = co.id
            LEFT JOIN cities ci ON h.city_id = ci.id
            WHERE h.id=?
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("findById hotel failed: " + e.getMessage(), e);
        }
    }

    private static Hotel map(ResultSet rs) throws Exception {
        Hotel h = new Hotel();
        h.setId(rs.getInt("id"));
        h.setName(rs.getString("name"));
        h.setCountryId(rs.getInt("country_id"));
        h.setCityId(rs.getInt("city_id"));
        h.setAddressLine(rs.getString("address_line"));
        h.setPhone(rs.getString("phone"));
        h.setEmail(rs.getString("email"));
        h.setImagePath(rs.getString("image_path"));
        h.setCountryName(rs.getString("country_name"));
        h.setCityName(rs.getString("city_name"));
        return h;
    }
}

