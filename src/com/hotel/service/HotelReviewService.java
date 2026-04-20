package com.hotel.service;

import com.hotel.database.dao.HotelReviewDAO;
import com.hotel.model.HotelReview;
import com.hotel.model.Reservation;

import java.time.LocalDate;
import java.util.List;

public class HotelReviewService {
    private final HotelReviewDAO hotelReviewDAO = new HotelReviewDAO();

    public int addReview(Reservation reservation, int customerId, int stars, String comment) {
        validateReviewInput(reservation, customerId, stars, comment);
        if (hotelReviewDAO.existsForReservation(reservation.getId())) {
            throw new IllegalArgumentException("You have already reviewed this reservation.");
        }
        HotelReview review = new HotelReview();
        review.setReservationId(reservation.getId());
        review.setHotelId(reservation.getHotelId());
        review.setCustomerId(customerId);
        review.setStars(stars);
        review.setComment(comment == null ? "" : comment.trim());
        return hotelReviewDAO.add(review);
    }

    public boolean hasReviewForReservation(int reservationId) {
        return hotelReviewDAO.existsForReservation(reservationId);
    }

    public List<HotelReview> getReviewsForHotel(int hotelId) {
        return hotelReviewDAO.findByHotelId(hotelId);
    }

    public double getAverageStarsForHotel(int hotelId) {
        return hotelReviewDAO.getAverageStarsForHotel(hotelId);
    }

    private void validateReviewInput(Reservation reservation, int customerId, int stars, String comment) {
        if (reservation == null) throw new IllegalArgumentException("Select a reservation first.");
        if (reservation.getCustomerId() != customerId) throw new IllegalArgumentException("This reservation does not belong to current user.");
        if (reservation.getHotelId() == null || reservation.getHotelId() <= 0) throw new IllegalArgumentException("Hotel is missing for this reservation.");
        if (reservation.getStatus() == Reservation.Status.CANCELLED) throw new IllegalArgumentException("Cancelled reservations cannot be reviewed.");
        if (!reservation.getCheckOutDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("You can review after check-out date.");
        }
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("Star rating must be between 1 and 5.");
        if (comment == null || comment.trim().length() < 3) throw new IllegalArgumentException("Comment must be at least 3 characters.");
    }
}
