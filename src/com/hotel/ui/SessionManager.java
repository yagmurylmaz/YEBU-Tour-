package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.User;
import javafx.scene.control.Alert;

public class SessionManager {

    private static SessionManager instance;
    private User loggedInUser;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void logout() {
        this.loggedInUser = null;
    }

    public boolean isAdmin() {
        return loggedInUser != null && "ADMIN".equals(loggedInUser.getRole());
    }

    public boolean ensureAdminAccess() {
        if (isAdmin()) return true;
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Access Denied");
        alert.setHeaderText(null);
        alert.setContentText("This page is for admin users only.");
        alert.showAndWait();
        logout();
        MainApp.navigateTo("login.fxml");
        return false;
    }
}
