package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.User;
import com.hotel.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;
import java.util.prefs.Preferences;

public class LoginController {
    private static final String PREF_REMEMBER_ME = "login.rememberMe";
    private static final String PREF_REMEMBERED_EMAIL = "login.rememberedEmail";

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();
    private final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

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
            persistRememberMe();
            SessionManager.getInstance().setLoggedInUser(user);
            if (SessionManager.isAdminRole(user.getRole())) {
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
        loadRememberMe();
        tryAutoLogin();
    }

    private void loadRememberMe() {
        boolean remembered = prefs.getBoolean(PREF_REMEMBER_ME, false);
        String rememberedEmail = prefs.get(PREF_REMEMBERED_EMAIL, "");
        if (rememberMeCheckBox != null) {
            rememberMeCheckBox.setSelected(remembered);
        }
        if (remembered && rememberedEmail != null && !rememberedEmail.isBlank()) {
            emailField.setText(rememberedEmail);
            passwordField.requestFocus();
        }
    }

    private void persistRememberMe() {
        boolean shouldRemember = rememberMeCheckBox != null && rememberMeCheckBox.isSelected();
        prefs.putBoolean(PREF_REMEMBER_ME, shouldRemember);
        if (shouldRemember) {
            prefs.put(PREF_REMEMBERED_EMAIL, emailField.getText().trim());
        } else {
            prefs.remove(PREF_REMEMBERED_EMAIL);
        }
    }

    private void tryAutoLogin() {
        boolean remembered = prefs.getBoolean(PREF_REMEMBER_ME, false);
        if (!remembered) return;
        String rememberedEmail = prefs.get(PREF_REMEMBERED_EMAIL, "").trim();
        if (rememberedEmail.isEmpty()) return;
        Optional<User> userOpt = authService.findByEmailForRememberMe(rememberedEmail);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        SessionManager.getInstance().setLoggedInUser(user);
        if (SessionManager.isAdminRole(user.getRole())) {
            MainApp.navigateTo("admin-dashboard.fxml");
        } else {
            MainApp.navigateTo("customer-dashboard.fxml");
        }
    }
}
