package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.service.PasswordResetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button sendButton;

    private final PasswordResetService passwordResetService = new PasswordResetService();

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }

    @FXML
    private void handleSend() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        sendButton.setDisable(true);
        String msg = passwordResetService.requestPasswordReset(emailField.getText());
        sendButton.setDisable(false);

        if (msg.startsWith("If an account exists")) {
            successLabel.setText(msg);
            successLabel.setVisible(true);
        } else {
            showError(msg);
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
}
