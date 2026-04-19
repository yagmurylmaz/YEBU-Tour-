package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.*;
import com.hotel.service.ReservationService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationController {
    @FXML private Label labelRoomNo;
    @FXML private Label labelRoomType;
    @FXML private Label labelCheckIn;
    @FXML private Label labelCheckOut;
    @FXML private Label labelNights;
    @FXML private Label labelRoomPrice;
    @FXML private Label labelTotalPrice;
    @FXML private CheckBox cbBreakfast;
    @FXML private CheckBox cbGym;
    @FXML private CheckBox cbPool;
    @FXML private CheckBox cbParking;
    @FXML private Label labelBreakfastPrice;
    @FXML private Label labelGymPrice;
    @FXML private Label labelPoolPrice;
    @FXML private Label labelParkingPrice;

    private final ReservationService reservationService = new ReservationService();

    private Room      room;
    private LocalDate checkIn;
    private LocalDate checkOut;

    @FXML
    private void initialize() {
        room     = RoomSearchController.getSelectedRoom();
        checkIn  = RoomSearchController.getSelectedCheckIn();
        checkOut = RoomSearchController.getSelectedCheckOut();

        if (room == null) {
            MainApp.navigateTo("room-search.fxml");
            return;
        }
        labelRoomNo.setText(room.getRoomNumber());
        labelRoomType.setText(room.getRoomTypeDisplay());
        labelCheckIn.setText(checkIn.toString());
        labelCheckOut.setText(checkOut.toString());
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        labelNights.setText(nights + " night(s)");
        labelRoomPrice.setText(room.getFormattedPrice() + " × " + nights + " night(s)");
        labelBreakfastPrice.setText("+ ₺" + BreakfastService.PRICE + " / night(s)");
        labelGymPrice.setText("+ ₺" + GymService.PRICE + " / night(s)");
        labelPoolPrice.setText("+ ₺" + PoolService.PRICE + " / night(s)");
        labelParkingPrice.setText("+ ₺" + ParkingService.PRICE + " / night(s)");
        cbBreakfast.setOnAction(e -> updateTotal());
        cbGym.setOnAction(e -> updateTotal());
        cbPool.setOnAction(e -> updateTotal());
        cbParking.setOnAction(e -> updateTotal());

        updateTotal();
    }

    private void updateTotal() {
        double total = reservationService.calculateTotalPrice(
            room, checkIn, checkOut, getSelectedServices()
        );
        labelTotalPrice.setText(String.format("₺%.2f", total));
    }

    private List<Service> getSelectedServices() {
        List<Service> services = new ArrayList<>();
        if (cbBreakfast.isSelected()) services.add(new BreakfastService());
        if (cbGym.isSelected())       services.add(new GymService());
        if (cbPool.isSelected())      services.add(new PoolService());
        if (cbParking.isSelected())   services.add(new ParkingService());
        return services;
    }

    @FXML
    private void handleConfirmReservation() {
        int customerId = SessionManager.getInstance().getLoggedInUser().getId();
        List<Service> services = getSelectedServices();

        double total = reservationService.calculateTotalPrice(room, checkIn, checkOut, services);

        Reservation reservation = new Reservation(
            customerId, room.getId(), checkIn, checkOut, total
        );

        int newId = reservationService.createReservation(reservation, services);

        if (newId > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Reservation Success");
            alert.setHeaderText("Your reservation has been received! 🎉");
            alert.setContentText(
                "Reservation No: #" + newId + "\n" +
                "Room: " + room.getRoomNumber() + " (" + room.getRoomTypeDisplay() + ")\n" +
                "Check-in: " + checkIn + "  |  Check-out: " + checkOut + "\n" +
                "Total Amount: ₺" + String.format("%.2f", total) + "\n\n" +
                "Your reservation is pending approval."
            );
            alert.showAndWait();
            MainApp.navigateTo("my-reservations.fxml");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Reservation could not be created.");
            alert.setContentText("Please try again or contact support.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleBack() {
        MainApp.navigateTo("room-search.fxml");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }
}
