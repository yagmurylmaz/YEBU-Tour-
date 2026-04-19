package com.hotel;

import com.hotel.database.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {

    private static final String FXML_BASE = "/com/hotel/fxml/";
    private static final String STYLESHEET = "/com/hotel/css/styles.css";
    private static final String APP_ICON = "/com/hotel/images/logo-yebu.png";

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
            Parent root = FXMLLoader.load(
                Objects.requireNonNull(
                    MainApp.class.getResource(FXML_BASE + fxmlFile)
                )
            );
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                Objects.requireNonNull(
                    MainApp.class.getResource(STYLESHEET)
                ).toExternalForm()
            );
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("[MainApp] Screen loading error (" + fxmlFile + "): " + e.getMessage());
            e.printStackTrace();
        }
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
