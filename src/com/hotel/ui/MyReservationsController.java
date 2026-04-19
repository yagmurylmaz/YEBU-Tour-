package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Reservation;
import com.hotel.service.ReservationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

public class MyReservationsController {

    @FXML private TableView<Reservation>           reservationTable;
    @FXML private TableColumn<Reservation, String> colId;
    @FXML private TableColumn<Reservation, String> colRoom;
    @FXML private TableColumn<Reservation, String> colCheckIn;
    @FXML private TableColumn<Reservation, String> colCheckOut;
    @FXML private TableColumn<Reservation, String> colNights;
    @FXML private TableColumn<Reservation, String> colTotal;
    @FXML private TableColumn<Reservation, String> colStatus;
    @FXML private Label infoLabel;

    private final ReservationService reservationService = new ReservationService();

    @FXML
    private void initialize() {
        setupTableColumns();
        loadReservations();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(d ->
            new SimpleStringProperty("#" + d.getValue().getId()));
        colRoom.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoomNumber()
                + " (" + d.getValue().getRoomType() + ")"));
        colCheckIn.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCheckInDate().toString()));
        colCheckOut.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCheckOutDate().toString()));
        colNights.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getNightCount() + " night(s)"));
        colTotal.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getFormattedPrice()));
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getStatusDisplay()));
        reservationTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Reservation res, boolean empty) {
                super.updateItem(res, empty);
                if (res == null || empty) {
                    setStyle("");
                } else {
                    switch (res.getStatus()) {
                        case APPROVED   -> setStyle("-fx-background-color: #e8f5e9;");
                        case CANCELLED  -> setStyle("-fx-background-color: #ffebee;");
                        default         -> setStyle("");
                    }
                }
            }
        });
    }

    private void loadReservations() {
        int customerId = SessionManager.getInstance().getLoggedInUser().getId();
        List<Reservation> list = reservationService.getCustomerReservations(customerId);
        reservationTable.setItems(FXCollections.observableArrayList(list));
        infoLabel.setText("Total " + list.size() + " reservation(s) found.");
    }

    @FXML
    private void handleCancelReservation() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select the reservation you want to cancel.");
            return;
        }
        if (selected.getStatus() != Reservation.Status.PENDING) {
            showAlert(Alert.AlertType.WARNING, "Cannot Cancel",
                "Only reservations in 'Pending' status can be cancelled.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Reservation");
        confirm.setHeaderText("Reservation #" + selected.getId() + " will be cancelled.");
        confirm.setContentText("Do you confirm this action?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = reservationService.cancelReservation(selected.getId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Reservation cancelled.");
                loadReservations();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Cancel operation could not be completed.");
            }
        }
    }

    @FXML private void handleBack()   { MainApp.navigateTo("customer-dashboard.fxml"); }
    @FXML private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
