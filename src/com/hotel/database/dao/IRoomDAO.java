package com.hotel.database.dao;

import com.hotel.model.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IRoomDAO {
    int save(Room room);
    boolean update(Room room);
    boolean delete(int roomId);
    List<Room> findAll();
    Optional<Room> findById(int roomId);
    List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, String roomType);
}
