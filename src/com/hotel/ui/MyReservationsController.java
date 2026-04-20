package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Reservation;
import com.hotel.service.HotelReviewService;
import com.hotel.service.ReservationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.Arrays;
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
    @FXML private Label selectedReservationLabel;
    @FXML private Label reviewEligibilityLabel;
    @FXML private ToggleButton star1Button;
    @FXML private ToggleButton star2Button;
    @FXML private ToggleButton star3Button;
    @FXML private ToggleButton star4Button;
    @FXML private ToggleButton star5Button;
    @FXML private TextArea reviewCommentArea;

    private final ReservationService reservationService = new ReservationService();
    private final HotelReviewService hotelReviewService = new HotelReviewService();
    private final ToggleGroup starsGroup = new ToggleGroup();

    @FXML
    private void initialize() {
        setupTableColumns();
        setupReviewPanel();
        loadReservations();
        reservationTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> refreshReviewPanel(selected));
        refreshReviewPanel(null);
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

    @FXML
    private void handleAddReview() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a reservation to review.");
            return;
        }
        if (!selected.getCheckOutDate().isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Too Early", "You can add a review after check-out date.");
            return;
        }
        if (hotelReviewService.hasReviewForReservation(selected.getId())) {
            showAlert(Alert.AlertType.INFORMATION, "Already Reviewed", "You already reviewed this reservation.");
            return;
        }
        Toggle selectedStar = starsGroup.getSelectedToggle();
        if (selectedStar == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Rating", "Please choose a star rating.");
            return;
        }
        int stars = (int) selectedStar.getUserData();
        String comment = reviewCommentArea == null ? "" : reviewCommentArea.getText();

        int customerId = SessionManager.getInstance().getLoggedInUser().getId();
        try {
            int reviewId = hotelReviewService.addReview(selected, customerId, stars, comment);
            if (reviewId > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Thanks, your review has been saved.");
                clearReviewInputs();
                refreshReviewPanel(selected);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Review could not be saved.");
            }
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Review could not be saved.");
        }
    }

    private void setupReviewPanel() {
        List<ToggleButton> stars = Arrays.asList(star1Button, star2Button, star3Button, star4Button, star5Button);
        for (int i = 0; i < stars.size(); i++) {
            ToggleButton button = stars.get(i);
            if (button == null) continue;
            button.setToggleGroup(starsGroup);
            button.setUserData(i + 1);
        }
    }

    private void refreshReviewPanel(Reservation reservation) {
        if (selectedReservationLabel == null || reviewEligibilityLabel == null || reviewCommentArea == null) return;
        clearReviewInputs();
        if (reservation == null) {
            selectedReservationLabel.setText("Select a reservation to review");
            reviewEligibilityLabel.setText("You can review your stay after check-out date.");
            setReviewInputsDisabled(true);
            return;
        }
        selectedReservationLabel.setText("Reservation #" + reservation.getId() + " | Room " + reservation.getRoomNumber());
        boolean afterCheckOut = reservation.getCheckOutDate().isBefore(LocalDate.now());
        boolean cancelled = reservation.getStatus() == Reservation.Status.CANCELLED;
        boolean alreadyReviewed = hotelReviewService.hasReviewForReservation(reservation.getId());
        boolean canReview = afterCheckOut && !cancelled && !alreadyReviewed;

        if (!afterCheckOut) {
            reviewEligibilityLabel.setText("Review will open after " + reservation.getCheckOutDate() + ".");
        } else if (cancelled) {
            reviewEligibilityLabel.setText("Cancelled reservations cannot be reviewed.");
        } else if (alreadyReviewed) {
            reviewEligibilityLabel.setText("You already reviewed this reservation.");
        } else {
            reviewEligibilityLabel.setText("Tell us about your hotel experience.");
        }
        setReviewInputsDisabled(!canReview);
    }

    private void setReviewInputsDisabled(boolean disabled) {
        if (reviewCommentArea != null) reviewCommentArea.setDisable(disabled);
        for (Toggle t : starsGroup.getToggles()) {
            if (t instanceof ToggleButton tb) tb.setDisable(disabled);
        }
    }

    private void clearReviewInputs() {
        starsGroup.selectToggle(null);
        if (reviewCommentArea != null) reviewCommentArea.clear();
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
