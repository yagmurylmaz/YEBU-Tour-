package com.hotel.service;

import com.hotel.database.dao.IRoomDAO;
import com.hotel.database.dao.RoomDAO;
import com.hotel.model.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class RoomService {

    private final IRoomDAO roomDAO;

    public RoomService() {
        this.roomDAO = new RoomDAO();
    }

    public int addRoom(Room room) {
        validateRoom(room);
        return roomDAO.save(room);
    }

    public boolean updateRoom(Room room) {
        validateRoom(room);
        return roomDAO.update(room);
    }

    public boolean deleteRoom(int roomId) {
        return roomDAO.delete(roomId);
    }

    public List<Room> getAllRooms() {
        return roomDAO.findAll();
    }

    public List<Room> getRoomsByHotelId(int hotelId) {
        return roomDAO.findByHotelId(hotelId);
    }

    public Optional<Room> getRoomById(int roomId) {
        return roomDAO.findById(roomId);
    }

    public List<Room> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut, String roomType) {
        return roomDAO.findAvailableRooms(checkIn, checkOut, roomType);
    }

    public List<Room> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut, String roomType, Integer hotelId) {
        return roomDAO.findAvailableRooms(checkIn, checkOut, roomType, hotelId);
    }

    private void validateRoom(Room room) {
        if (room.getRoomNumber() == null || room.getRoomNumber().isBlank())
            throw new IllegalArgumentException("Room number cannot be empty.");
        if (room.getRoomType() == null)
            throw new IllegalArgumentException("Room type must be selected.");
        if (room.getPricePerNight() <= 0)
            throw new IllegalArgumentException("Nightly price must be greater than zero.");
        if (room.getCapacity() <= 0)
            throw new IllegalArgumentException("Capacity must be at least 1 guest.");
    }
}
