package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.User;
import javafx.scene.control.Alert;

public class SessionManager {

    private static SessionManager instance;
    private User loggedInUser;
    private boolean darkModeEnabled;

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

    public boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public void setDarkModeEnabled(boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }

    public boolean isAdmin() {
        return isAdminRole(loggedInUser != null ? loggedInUser.getRole() : null);
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

    public static boolean isAdminRole(String role) {
        return role != null && "ADMIN".equalsIgnoreCase(role.trim());
    }
}
