package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.service.PasswordResetService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ResetPasswordController {

    @FXML private TextField tokenField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private final PasswordResetService passwordResetService = new PasswordResetService();

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }

    @FXML
    private void handleReset() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        String result = passwordResetService.resetPassword(
            tokenField.getText(),
            passwordField.getText(),
            confirmPasswordField.getText()
        );

        if ("OK".equals(result)) {
            successLabel.setText("Your password has been updated. You can sign in now.");
            successLabel.setVisible(true);
            tokenField.clear();
            passwordField.clear();
            confirmPasswordField.clear();
            new Thread(() -> {
                try {
                    Thread.sleep(1800);
                } catch (InterruptedException ignored) {
                }
                javafx.application.Platform.runLater(() -> MainApp.navigateTo("login.fxml"));
            }).start();
        } else {
            errorLabel.setText(result);
            errorLabel.setVisible(true);
        }
    }

    @FXML
    private void handleBackToLogin() {
        MainApp.navigateTo("login.fxml");
    }
}
