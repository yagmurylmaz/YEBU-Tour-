package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Room;
import com.hotel.service.ReservationService;
import com.hotel.service.RoomService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.List;

public class RoomSearchController {

    @FXML private DatePicker    checkInDatePicker;
    @FXML private DatePicker    checkOutDatePicker;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private TableView<Room>  roomTable;
    @FXML private TableColumn<Room, String> colRoomNo;
    @FXML private TableColumn<Room, String> colType;
    @FXML private TableColumn<Room, String> colPrice;
    @FXML private TableColumn<Room, String> colCapacity;
    @FXML private TableColumn<Room, String> colDescription;
    @FXML private Label statusLabel;

    private final RoomService        roomService        = new RoomService();
    private final ReservationService reservationService = new ReservationService();

    private static Room selectedRoom;
    private static LocalDate selectedCheckIn;
    private static LocalDate selectedCheckOut;

    @FXML
    private void initialize() {
        roomTypeCombo.setItems(FXCollections.observableArrayList(
            "ALL", "SINGLE", "DOUBLE", "SUITE", "DELUXE"
        ));
        roomTypeCombo.setValue("ALL");
        colRoomNo.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoomNumber()));
        colType.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoomTypeDisplay()));
        colPrice.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getFormattedPrice() + " / night(s)"));
        colCapacity.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCapacity() + " guests"));
        colDescription.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getDescription()));
        checkInDatePicker.setValue(LocalDate.now());
        checkOutDatePicker.setValue(LocalDate.now().plusDays(1));

        statusLabel.setText("Select dates and room type to search.");
    }

    @FXML
    private void handleSearch() {
        LocalDate checkIn  = checkInDatePicker.getValue();
        LocalDate checkOut = checkOutDatePicker.getValue();
        try {
            reservationService.validateDates(checkIn, checkOut);
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Date Error", e.getMessage());
            return;
        }

        String roomType = roomTypeCombo.getValue();
        List<Room> rooms = roomService.searchAvailableRooms(checkIn, checkOut, roomType);

        roomTable.setItems(FXCollections.observableArrayList(rooms));

        if (rooms.isEmpty()) {
            statusLabel.setText("No available rooms for the selected date range.");
        } else {
            statusLabel.setText(rooms.size() + " available rooms listed. "
                + "Select a room to proceed with reservation.");
        }
    }

    @FXML
    private void handleMakeReservation() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Room Not Selected",
                "Please select a room you want to reserve.");
            return;
        }
        selectedRoom      = selected;
        selectedCheckIn   = checkInDatePicker.getValue();
        selectedCheckOut  = checkOutDatePicker.getValue();

        MainApp.navigateTo("reservation.fxml");
    }

    @FXML
    private void handleBack() {
        MainApp.navigateTo("customer-dashboard.fxml");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }

    public static Room getSelectedRoom()         { return selectedRoom; }
    public static LocalDate getSelectedCheckIn() { return selectedCheckIn; }
    public static LocalDate getSelectedCheckOut(){ return selectedCheckOut; }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
