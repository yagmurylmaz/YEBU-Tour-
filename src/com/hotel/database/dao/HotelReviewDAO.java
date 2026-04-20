package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.HotelReview;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HotelReviewDAO {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public int add(HotelReview review) {
        String sql = """
            INSERT INTO hotel_reviews (reservation_id, hotel_id, customer_id, stars, comment, created_at)
            VALUES (?,?,?,?,?,?)
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, review.getReservationId());
            ps.setInt(2, review.getHotelId());
            ps.setInt(3, review.getCustomerId());
            ps.setInt(4, review.getStars());
            ps.setString(5, review.getComment());
            ps.setString(6, LocalDateTime.now().format(TS_FORMAT));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            return -1;
        } catch (Exception e) {
            throw new IllegalStateException("add review failed: " + e.getMessage(), e);
        }
    }

    public boolean existsForReservation(int reservationId) {
        String sql = "SELECT COUNT(*) FROM hotel_reviews WHERE reservation_id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("exists review failed: " + e.getMessage(), e);
        }
    }

    public List<HotelReview> findByHotelId(int hotelId) {
        String sql = """
            SELECT hr.id, hr.reservation_id, hr.hotel_id, hr.customer_id, hr.stars, hr.comment, hr.created_at,
                   u.full_name AS customer_name, h.name AS hotel_name
            FROM hotel_reviews hr
            JOIN users u ON hr.customer_id = u.id
            JOIN hotels h ON hr.hotel_id = h.id
            WHERE hr.hotel_id = ?
            ORDER BY hr.id DESC
            """;
        List<HotelReview> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, hotelId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("find reviews failed: " + e.getMessage(), e);
        }
        return list;
    }

    public double getAverageStarsForHotel(int hotelId) {
        String sql = "SELECT COALESCE(AVG(stars), 0) FROM hotel_reviews WHERE hotel_id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, hotelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
            return 0.0;
        } catch (Exception e) {
            throw new IllegalStateException("average stars failed: " + e.getMessage(), e);
        }
    }

    private static HotelReview map(ResultSet rs) throws Exception {
        HotelReview r = new HotelReview();
        r.setId(rs.getInt("id"));
        r.setReservationId(rs.getInt("reservation_id"));
        r.setHotelId(rs.getInt("hotel_id"));
        r.setCustomerId(rs.getInt("customer_id"));
        r.setStars(rs.getInt("stars"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(rs.getString("created_at"));
        r.setCustomerName(rs.getString("customer_name"));
        r.setHotelName(rs.getString("hotel_name"));
        return r;
    }
}
