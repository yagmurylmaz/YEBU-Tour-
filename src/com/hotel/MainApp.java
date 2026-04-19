package com.hotel;

import com.hotel.database.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainApp extends Application {

    private static final String FXML_BASE = "/com/hotel/fxml/";
    private static final String STYLESHEET = "/com/hotel/css/styles.css";
    private static final String APP_ICON = "/com/hotel/images/logo-yebu.png";
    private static final int MAX_PARENT_WALK = 12;

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("YEBU Tour - Reservation System");
        var iconUrl = MainApp.class.getResource(APP_ICON);
        if (iconUrl != null) {
            primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        DatabaseInitializer.initialize();
        navigateTo("login.fxml");

        primaryStage.show();
    }

    public static void navigateTo(String fxmlFile) {
        try {
            URL fxmlUrl = resolveResource(FXML_BASE + fxmlFile);
            if (fxmlUrl == null) {
                throw new IllegalStateException("FXML not found: " + FXML_BASE + fxmlFile);
            }
            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);
            URL cssUrl = resolveResource(STYLESHEET);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("[MainApp] Stylesheet not found: " + STYLESHEET);
            }
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException | IllegalStateException e) {
            System.err.println("[MainApp] Screen loading error (" + fxmlFile + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static URL resolveResource(String classpathPath) {
        URL cpUrl = MainApp.class.getResource(classpathPath);
        if (cpUrl != null) return cpUrl;

        String relative = classpathPath.startsWith("/") ? classpathPath.substring(1) : classpathPath;
        Path dir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int i = 0; i < MAX_PARENT_WALK && dir != null; i++) {
            Path candidate = dir.resolve("resources").resolve(relative);
            if (Files.isRegularFile(candidate)) {
                try {
                    return candidate.toUri().toURL();
                } catch (Exception ignored) {
                    return null;
                }
            }
            dir = dir.getParent();
        }
        return null;
    }

    @Override
    public void stop() {
        System.out.println("[MainApp] Application is closing...");
        try {
            com.hotel.database.DatabaseConnection.getInstance().closeConnection();
        } catch (Exception e) {
            System.err.println("[MainApp] Shutdown error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
