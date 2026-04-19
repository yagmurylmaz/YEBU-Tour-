package com.hotel.service;

import com.hotel.database.dao.IReservationDAO;
import com.hotel.database.dao.ReservationDAO;
import com.hotel.model.Reservation;
import com.hotel.model.Reservation.Status;
import com.hotel.model.Room;
import com.hotel.model.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService implements IReservationService {

    private final IReservationDAO reservationDAO;

    public ReservationService() {
        this.reservationDAO = new ReservationDAO();
    }

    @Override
    public double calculateTotalPrice(Room room, LocalDate checkIn,
                                      LocalDate checkOut, List<Service> services) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double roomTotal = nights * room.getPricePerNight();

        double serviceTotal = 0.0;
        if (services != null) {
            serviceTotal = services.stream()
                    .mapToDouble(svc -> svc.getPrice() * nights)
                    .sum();
        }
        return roomTotal + serviceTotal;
    }

    @Override
    public int createReservation(Reservation reservation, List<Service> services) {
        validateDates(reservation.getCheckInDate(), reservation.getCheckOutDate());
        return reservationDAO.save(reservation, services);
    }

    public void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null)
            throw new IllegalArgumentException("Check-in and check-out dates must be selected.");
        if (checkIn.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Check-in date cannot be in the past.");
        if (!checkOut.isAfter(checkIn))
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        if (ChronoUnit.DAYS.between(checkIn, checkOut) < 1)
            throw new IllegalArgumentException("Minimum stay is 1 night.");
    }

    @Override
    public List<Reservation> getCustomerReservations(int customerId) {
        return reservationDAO.findByCustomerId(customerId);
    }

    @Override
    public List<Reservation> getAllReservations() {
        return reservationDAO.findAll();
    }

    @Override
    public boolean approveReservation(int reservationId) {
        return reservationDAO.updateStatus(reservationId, Status.APPROVED);
    }

    @Override
    public boolean cancelReservation(int reservationId) {
        return reservationDAO.updateStatus(reservationId, Status.CANCELLED);
    }
}
