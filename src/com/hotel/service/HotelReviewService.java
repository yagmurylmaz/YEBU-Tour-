package com.hotel.service;

import com.hotel.database.DatabaseConnection;
import com.hotel.database.dao.HotelReviewDAO;
import com.hotel.model.HotelReview;
import com.hotel.model.Reservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HotelReviewService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);
    private static final Map<Integer, ReviewCacheEntry> REVIEW_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, AvgCacheEntry> AVG_CACHE = new ConcurrentHashMap<>();

    private final HotelReviewDAO hotelReviewDAO = new HotelReviewDAO();
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<HotelReview> getReviewsForHotel(int hotelId) {
        ReviewCacheEntry cached = REVIEW_CACHE.get(hotelId);
        if (cached != null && Instant.now().isBefore(cached.cachedAt.plus(CACHE_TTL))) {
            return cached.reviews;
        }
        List<HotelReview> fresh = hotelReviewDAO.findByHotelId(hotelId);
        REVIEW_CACHE.put(hotelId, new ReviewCacheEntry(fresh));
        return fresh;
    }

    public double getAverageStarsForHotel(int hotelId) {
        AvgCacheEntry cached = AVG_CACHE.get(hotelId);
        if (cached != null && Instant.now().isBefore(cached.cachedAt.plus(CACHE_TTL))) {
            return cached.avg;
        }
        double fresh = hotelReviewDAO.getAverageStarsForHotel(hotelId);
        AVG_CACHE.put(hotelId, new AvgCacheEntry(fresh));
        return fresh;
    }

    public int addReview(Reservation reservation, int customerId, int stars, String comment) {
        if (reservation == null) throw new IllegalArgumentException("Select a reservation.");
        if (reservation.getCustomerId() != customerId) throw new IllegalArgumentException("Reservation does not belong to current user.");
        if (reservation.getStatus() == Reservation.Status.CANCELLED) throw new IllegalArgumentException("Cancelled reservations cannot be reviewed.");
        if (!reservation.getCheckOutDate().isBefore(LocalDate.now())) throw new IllegalArgumentException("You can review only after check-out date.");
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("Stars must be between 1 and 5.");
        String cleanComment = comment == null ? "" : comment.trim();
        if (cleanComment.length() < 3) throw new IllegalArgumentException("Comment must be at least 3 characters.");
        if (hotelReviewDAO.existsForReservation(reservation.getId())) {
            throw new IllegalArgumentException("You already reviewed this reservation.");
        }

        Integer hotelId = findHotelIdForReservation(reservation.getId(), customerId);
        if (hotelId == null || hotelId <= 0) throw new IllegalArgumentException("Could not resolve hotel for reservation.");

        HotelReview review = new HotelReview();
        review.setReservationId(reservation.getId());
        review.setHotelId(hotelId);
        review.setCustomerId(customerId);
        review.setStars(stars);
        review.setComment(cleanComment);
        int id = hotelReviewDAO.add(review);
        invalidateHotelReviewCache(hotelId);
        return id;
    }

    public boolean hasReviewForReservation(int reservationId) {
        return hotelReviewDAO.existsForReservation(reservationId);
    }

    private Integer findHotelIdForReservation(int reservationId, int customerId) {
        String sql = """
            SELECT rm.hotel_id
            FROM reservations r
            JOIN rooms rm ON r.room_id = rm.id
            WHERE r.id = ? AND r.customer_id = ?
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int hotelId = rs.getInt("hotel_id");
                return rs.wasNull() ? null : hotelId;
            }
        } catch (Exception e) {
            throw new IllegalStateException("find hotel for reservation failed: " + e.getMessage(), e);
        }
    }

    private static void invalidateHotelReviewCache(int hotelId) {
        REVIEW_CACHE.remove(hotelId);
        AVG_CACHE.remove(hotelId);
    }

    private static final class ReviewCacheEntry {
        private final List<HotelReview> reviews;
        private final Instant cachedAt;
        private ReviewCacheEntry(List<HotelReview> reviews) {
            this.reviews = List.copyOf(reviews);
            this.cachedAt = Instant.now();
        }
    }

    private static final class AvgCacheEntry {
        private final double avg;
        private final Instant cachedAt;
        private AvgCacheEntry(double avg) {
            this.avg = avg;
            this.cachedAt = Instant.now();
        }
    }
}
