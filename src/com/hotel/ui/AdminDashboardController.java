package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Reservation;
import com.hotel.service.ReportService;
import com.hotel.service.ReservationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminDashboardController {
    @FXML private Label labelTotalRes;
    @FXML private Label labelPending;
    @FXML private Label labelApproved;
    @FXML private Label labelCancelled;
    @FXML private Label labelRevenue;
    @FXML private TableView<Reservation>           reservationTable;
    @FXML private TableColumn<Reservation, String> colId;
    @FXML private TableColumn<Reservation, String> colCustomer;
    @FXML private TableColumn<Reservation, String> colRoom;
    @FXML private TableColumn<Reservation, String> colCheckIn;
    @FXML private TableColumn<Reservation, String> colCheckOut;
    @FXML private TableColumn<Reservation, String> colTotal;
    @FXML private TableColumn<Reservation, String> colStatus;
    @FXML private TableColumn<Reservation, String> colCreated;

    private final ReservationService reservationService = new ReservationService();
    private final ReportService      reportService      = new ReportService();

    @FXML
    private void initialize() {
        setupTableColumns();
        loadData();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(d ->
            new SimpleStringProperty("#" + d.getValue().getId()));
        colCustomer.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCustomerName()));
        colRoom.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoomNumber()
                + " / " + d.getValue().getRoomType()));
        colCheckIn.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCheckInDate().toString()));
        colCheckOut.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCheckOutDate().toString()));
        colTotal.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getFormattedPrice()));
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getStatusDisplay()));
        colCreated.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCreatedAt()));
        reservationTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Reservation r, boolean empty) {
                super.updateItem(r, empty);
                if (r == null || empty) setStyle("");
                else switch (r.getStatus()) {
                    case APPROVED  -> setStyle("-fx-background-color: #e8f5e9;");
                    case CANCELLED -> setStyle("-fx-background-color: #ffebee;");
                    default        -> setStyle("-fx-background-color: #fff8e1;");
                }
            }
        });
    }

    private void loadData() {
        List<Reservation> list = reservationService.getAllReservations();
        reservationTable.setItems(FXCollections.observableArrayList(list));
        Map<String, Object> summary = reportService.getAdminSummary();
        labelTotalRes.setText(summary.get("totalReservations").toString());
        labelPending.setText(summary.get("pending").toString());
        labelApproved.setText(summary.get("approved").toString());
        labelCancelled.setText(summary.get("cancelled").toString());
        labelRevenue.setText(String.format("₺%.2f", summary.get("totalRevenue")));
    }

    @FXML
    private void handleApprove() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarn("Please select a reservation."); return; }
        if (selected.getStatus() != Reservation.Status.PENDING) {
            showWarn("Only reservations in 'Pending' status can be approved.");
            return;
        }
        if (confirmAction("Reservation #" + selected.getId() + " will be approved. Do you confirm?")) {
            if (reservationService.approveReservation(selected.getId())) {
                showInfo("Reservation approved successfully.");
                loadData();
            }
        }
    }

    @FXML
    private void handleCancel() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarn("Please select a reservation."); return; }
        if (selected.getStatus() == Reservation.Status.CANCELLED) {
            showWarn("This reservation is already cancelled.");
            return;
        }
        if (confirmAction("Reservation #" + selected.getId() + " will be cancelled. Do you confirm?")) {
            if (reservationService.cancelReservation(selected.getId())) {
                showInfo("Reservation cancelled successfully.");
                loadData();
            }
        }
    }

    @FXML private void handleManageRooms()  { MainApp.navigateTo("admin-rooms.fxml"); }

    @FXML private void handleReports()      { MainApp.navigateTo("admin-report.fxml"); }

    @FXML private void handleRefresh()      { loadData(); }

    @FXML private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }

    private boolean confirmAction(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showWarn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }
}
