package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.Reservation;
import com.hotel.model.Room;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomDAO implements IRoomDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public int save(Room room) {
        room.setId(db.nextRoomId());
        db.rooms().add(room);
        db.persistAll();
        return room.getId();
    }

    @Override
    public boolean update(Room room) {
        Optional<Room> existingOpt = findById(room.getId());
        if (existingOpt.isEmpty()) return false;
        Room existing = existingOpt.get();
        existing.setRoomNumber(room.getRoomNumber());
        existing.setRoomType(room.getRoomType());
        existing.setPricePerNight(room.getPricePerNight());
        existing.setCapacity(room.getCapacity());
        existing.setDescription(room.getDescription());
        existing.setAvailable(room.isAvailable());
        db.persistAll();
        return true;
    }

    @Override
    public boolean delete(int roomId) {
        boolean hasActiveReservation = db.reservations().stream()
            .anyMatch(r -> r.getRoomId() == roomId && r.getStatus() != Reservation.Status.CANCELLED);
        if (hasActiveReservation) return false;
        boolean deleted = db.rooms().removeIf(r -> r.getId() == roomId);
        if (deleted) db.persistAll();
        return deleted;
    }

    @Override
    public List<Room> findAll() {
        return new ArrayList<>(db.rooms());
    }

    @Override
    public Optional<Room> findById(int roomId) {
        return db.rooms().stream().filter(r -> r.getId() == roomId).findFirst();
    }

    @Override
    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, String roomType) {
        List<Room> result = new ArrayList<>();
        for (Room room : db.rooms()) {
            if (!room.isAvailable()) continue;
            if (roomType != null && !"ALL".equalsIgnoreCase(roomType) &&
                !room.getRoomType().name().equalsIgnoreCase(roomType)) {
                continue;
            }
            boolean conflict = db.reservations().stream().anyMatch(r ->
                r.getRoomId() == room.getId() &&
                r.getStatus() != Reservation.Status.CANCELLED &&
                datesOverlap(checkIn, checkOut, r.getCheckInDate(), r.getCheckOutDate())
            );
            if (!conflict) result.add(room);
        }
        return result;
    }

    private boolean datesOverlap(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
        return startA.isBefore(endB) && endA.isAfter(startB);
    }
}
