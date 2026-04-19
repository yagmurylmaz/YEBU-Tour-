package com.hotel.database.dao;

import com.hotel.model.Reservation;
import com.hotel.model.Reservation.Status;
import com.hotel.model.Service;

import java.util.List;

public interface IReservationDAO {
    int save(Reservation reservation, List<Service> services);
    List<Reservation> findByCustomerId(int customerId);
    List<Reservation> findAll();
    boolean updateStatus(int reservationId, Status status);
}
