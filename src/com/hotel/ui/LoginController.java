package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.User;
import com.hotel.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            showError("Email and password fields cannot be empty.");
            return;
        }

        Optional<User> userOpt = authService.login(email, password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            SessionManager.getInstance().setLoggedInUser(user);
            if ("ADMIN".equals(user.getRole())) {
                MainApp.navigateTo("admin-dashboard.fxml");
            } else {
                MainApp.navigateTo("customer-dashboard.fxml");
            }
        } else {
            showError("Email or password is incorrect. Please try again.");
            passwordField.clear();
        }
    }

    @FXML
    private void handleGoToRegister() {
        MainApp.navigateTo("register.fxml");
    }

    @FXML
    private void handleForgotPassword() {
        MainApp.navigateTo("forgot-password.fxml");
    }

    @FXML
    private void handleResetPassword() {
        MainApp.navigateTo("reset-password.fxml");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
    }
}
