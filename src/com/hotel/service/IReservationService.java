package com.hotel.service;

import com.hotel.model.Reservation;
import com.hotel.model.Room;
import com.hotel.model.Service;

import java.time.LocalDate;
import java.util.List;

public interface IReservationService {

    double calculateTotalPrice(Room room, LocalDate checkIn,
                               LocalDate checkOut, List<Service> services);

    int createReservation(Reservation reservation, List<Service> services);

    List<Reservation> getCustomerReservations(int customerId);

    List<Reservation> getAllReservations();

    boolean approveReservation(int reservationId);

    boolean cancelReservation(int reservationId);
}
