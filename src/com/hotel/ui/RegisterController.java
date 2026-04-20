package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField     phoneField;
    @FXML private TextField     verificationCodeField;
    @FXML private Button        sendCodeButton;
    @FXML private Button        registerButton;
    @FXML private Label         errorLabel;
    @FXML private Label         successLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        if (verificationCodeField != null) verificationCodeField.setDisable(true);
        if (registerButton != null) registerButton.setDisable(true);
    }

    @FXML
    private void handleSendCode() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        String fullName        = fullNameField.getText().trim();
        String email           = emailField.getText().trim();
        String password        = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String phone           = phoneField.getText().trim();
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match. Please check again.");
            return;
        }

        if (sendCodeButton != null) sendCodeButton.setDisable(true);
        String result = authService.requestRegistrationVerificationCode(fullName, email, password, phone);
        if (sendCodeButton != null) sendCodeButton.setDisable(false);
        if ("OK".equals(result)) {
            if (verificationCodeField != null) verificationCodeField.setDisable(false);
            if (registerButton != null) registerButton.setDisable(false);
            showSuccess("Verification code sent to your email. Enter the code to complete registration.");
        } else {
            showError(result);
        }
    }

    @FXML
    private void handleRegister() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        String email = emailField.getText().trim();
        String code = verificationCodeField.getText() != null ? verificationCodeField.getText().trim() : "";
        String result = authService.verifyRegistrationCodeAndCreateUser(email, code);

        if ("OK".equals(result)) {
            showSuccess("Registration successful! Redirecting to login screen...");
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> MainApp.navigateTo("login.fxml"));
            }).start();
        } else {
            showError(result);
        }
    }

    @FXML
    private void handleBackToLogin() {
        MainApp.navigateTo("login.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
    }
}
