package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class CustomerDashboardController {

    @FXML private Label welcomeLabel;

    @FXML
    private void initialize() {
        User user = SessionManager.getInstance().getLoggedInUser();
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getFullName() + "!");
        }
    }

    @FXML
    private void handleSearchRooms() {
        MainApp.navigateTo("room-search.fxml");
    }

    @FXML
    private void handleMyReservations() {
        MainApp.navigateTo("my-reservations.fxml");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }
}
