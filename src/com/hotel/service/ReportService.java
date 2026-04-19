package com.hotel.service;

import com.hotel.database.dao.ReservationDAO;
import com.hotel.model.Reservation;
import com.hotel.model.Reservation.Status;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {

    private final ReservationDAO reservationDAO;
    private final ReservationService reservationService;

    public ReportService() {
        this.reservationDAO     = new ReservationDAO();
        this.reservationService = new ReservationService();
    }

    public Map<String, Object> getAdminSummary() {
        List<Reservation> all = reservationService.getAllReservations();

        long total     = all.size();
        long pending   = all.stream().filter(r -> r.getStatus() == Status.PENDING).count();
        long approved  = all.stream().filter(r -> r.getStatus() == Status.APPROVED).count();
        long cancelled = all.stream().filter(r -> r.getStatus() == Status.CANCELLED).count();
        double revenue = reservationDAO.getTotalRevenue();

        return Map.of(
            "totalReservations", total,
            "pending",   pending,
            "approved",  approved,
            "cancelled", cancelled,
            "totalRevenue", revenue
        );
    }

    public String generateReservationSummary(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("           YEBU TOUR                 \n");
        sb.append("      Reservation Summary / Invoice       \n");
        sb.append("═══════════════════════════════════════\n\n");
        sb.append(String.format("Reservation No : #%d%n", reservation.getId()));
        sb.append(String.format("Customer Name    : %s%n", reservation.getCustomerName()));
        sb.append(String.format("Room No         : %s%n", reservation.getRoomNumber()));
        sb.append(String.format("Room Type       : %s%n", reservation.getRoomType()));
        sb.append(String.format("Check-in Date   : %s%n", reservation.getCheckInDate()));
        sb.append(String.format("Check-out Date   : %s%n", reservation.getCheckOutDate()));
        sb.append(String.format("Nights    : %d night(s)%n", reservation.getNightCount()));
        sb.append(String.format("Status          : %s%n%n", reservation.getStatusDisplay()));
        sb.append("───────────────────────────────────────\n");
        sb.append(String.format("%-25s %8s%n", "Description", "Amount"));
        sb.append("───────────────────────────────────────\n");
        sb.append(String.format("%-25s %8s%n",
            "Room Charge (total)",
            reservation.getFormattedPrice()));
        sb.append("───────────────────────────────────────\n");
        sb.append(String.format("%-25s %8s%n", "TOTAL AMOUNT", reservation.getFormattedPrice()));
        sb.append("═══════════════════════════════════════\n");
        sb.append("Thank you for your reservation!\n");
        sb.append("Have a great stay. 🏨\n");
        return sb.toString();
    }

    public Map<String, Long> getReservationsByRoomType() {
        return reservationService.getAllReservations().stream()
            .collect(Collectors.groupingBy(
                r -> r.getRoomType() != null ? r.getRoomType() : "Unknown",
                Collectors.counting()
            ));
    }
}
