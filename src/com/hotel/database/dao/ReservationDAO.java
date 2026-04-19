package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.Reservation;
import com.hotel.model.Reservation.Status;
import com.hotel.model.Room;
import com.hotel.model.Service;
import com.hotel.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationDAO implements IReservationDAO {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public int save(Reservation reservation, List<Service> services) {
        if (!isRoomAvailableForReservation(reservation)) return -1;
        reservation.setId(db.nextReservationId());
        reservation.setStatus(Status.PENDING);
        reservation.setCreatedAt(LocalDateTime.now().format(TS_FORMAT));
        enrich(reservation);
        db.reservations().add(reservation);
        db.persistAll();
        return reservation.getId();
    }

    @Override
    public List<Reservation> findByCustomerId(int customerId) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation reservation : db.reservations()) {
            if (reservation.getCustomerId() == customerId) {
                enrich(reservation);
                result.add(reservation);
            }
        }
        return result;
    }

    @Override
    public List<Reservation> findAll() {
        List<Reservation> result = new ArrayList<>(db.reservations());
        result.forEach(this::enrich);
        return result;
    }

    @Override
    public boolean updateStatus(int reservationId, Status status) {
        Optional<Reservation> reservationOpt = db.reservations().stream()
            .filter(r -> r.getId() == reservationId)
            .findFirst();
        if (reservationOpt.isEmpty()) return false;
        reservationOpt.get().setStatus(status);
        db.persistAll();
        return true;
    }

    public double getTotalRevenue() {
        return db.reservations().stream()
            .filter(r -> r.getStatus() == Status.APPROVED)
            .mapToDouble(Reservation::getTotalPrice)
            .sum();
    }

    private boolean isRoomAvailableForReservation(Reservation reservation) {
        return db.reservations().stream().noneMatch(existing ->
            existing.getRoomId() == reservation.getRoomId() &&
            existing.getStatus() != Status.CANCELLED &&
            reservation.getCheckInDate().isBefore(existing.getCheckOutDate()) &&
            reservation.getCheckOutDate().isAfter(existing.getCheckInDate())
        );
    }

    private void enrich(Reservation reservation) {
        db.users().stream()
            .filter(u -> u.getId() == reservation.getCustomerId())
            .findFirst()
            .map(User::getFullName)
            .ifPresent(reservation::setCustomerName);

        db.rooms().stream()
            .filter(r -> r.getId() == reservation.getRoomId())
            .findFirst()
            .ifPresent(room -> {
                reservation.setRoomNumber(room.getRoomNumber());
                reservation.setRoomType(room.getRoomTypeDisplay());
            });
    }
}
