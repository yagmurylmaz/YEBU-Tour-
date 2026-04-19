package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Hotel;
import com.hotel.service.HotelService;
import com.hotel.ui.AdminRoomController;
import com.hotel.util.HotelImageStorage;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class AdminHotelController {
    @FXML private TextField hotelNameField;
    @FXML private ComboBox<String> countryCombo;
    @FXML private TextField cityField;
    @FXML private TextArea addressArea;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private ImageView hotelImagePreview;
    @FXML private Label statusLabel;

    @FXML private TableView<Hotel> hotelTable;
    @FXML private TableColumn<Hotel, String> colId;
    @FXML private TableColumn<Hotel, String> colName;
    @FXML private TableColumn<Hotel, String> colCountry;
    @FXML private TableColumn<Hotel, String> colCity;
    @FXML private TableColumn<Hotel, String> colAddress;
    @FXML private TableColumn<Hotel, String> colPhone;

    private final HotelService hotelService = new HotelService();
    private Hotel selectedHotel;
    private File selectedImageFile;

    @FXML
    private void initialize() {
        if (!SessionManager.getInstance().ensureAdminAccess()) return;
        setupTable();
        loadCountriesFromApi();
        loadHotels();

        hotelTable.getSelectionModel().selectedItemProperty().addListener((obs, old, h) -> {
            if (h != null) populateForm(h);
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colCountry.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCountryName()));
        colCity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCityName()));
        colAddress.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAddressLine()));
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone()));
    }

    private void loadCountriesFromApi() {
        try {
            List<String> countries = hotelService.getCountriesFromApi();
            countryCombo.setItems(FXCollections.observableArrayList(countries));
            statusLabel.setText("Loaded " + countries.size() + " country(ies) from API.");
        } catch (Exception e) {
            countryCombo.setItems(FXCollections.observableArrayList());
            statusLabel.setText("Country API is unavailable. Please try again.");
        }
    }

    private void loadHotels() {
        List<Hotel> hotels = hotelService.getAllHotels();
        hotelTable.setItems(FXCollections.observableArrayList(hotels));
        statusLabel.setText("Listed " + hotels.size() + " hotel(s).");
    }

    private void populateForm(Hotel h) {
        selectedHotel = h;
        selectedImageFile = null;
        hotelNameField.setText(h.getName());
        addressArea.setText(h.getAddressLine());
        phoneField.setText(h.getPhone());
        emailField.setText(h.getEmail());
        countryCombo.setValue(h.getCountryName());
        cityField.setText(h.getCityName());
        if (hotelImagePreview != null) {
            hotelImagePreview.setImage(HotelImageStorage.loadFxImage(h.getImagePath()));
        }
    }

    @FXML
    private void handleAddHotel() {
        try {
            Hotel h = buildFromForm();
            int id = hotelService.addHotel(h, countryCombo.getValue(), cityField.getText());
            if (id > 0) {
                saveSelectedPhotoIfAny(id, h);
                showInfo("Hotel added successfully. (ID: " + id + ")");
                handleClear();
                loadHotels();
            }
        } catch (IllegalArgumentException e) {
            showWarn(e.getMessage());
        } catch (Exception e) {
            showWarn("Hotel could not be added: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateHotel() {
        if (selectedHotel == null) { showWarn("Select a hotel from the table to update."); return; }
        try {
            Hotel h = buildFromForm();
            h.setId(selectedHotel.getId());
            h.setImagePath(selectedHotel.getImagePath());
            if (selectedImageFile != null) {
                String newPath = HotelImageStorage.copyToHotelFile(selectedHotel.getId(), selectedImageFile);
                String oldPath = selectedHotel.getImagePath();
                h.setImagePath(newPath);
                if (hotelService.updateHotel(h, countryCombo.getValue(), cityField.getText())) {
                    HotelImageStorage.deleteFileIfExists(oldPath);
                    showInfo("Hotel updated.");
                    handleClear();
                    loadHotels();
                    return;
                }
            }
            if (hotelService.updateHotel(h, countryCombo.getValue(), cityField.getText())) {
                showInfo("Hotel updated.");
                handleClear();
                loadHotels();
            }
        } catch (IllegalArgumentException e) {
            showWarn(e.getMessage());
        } catch (IOException e) {
            showWarn("Photo could not be saved: " + e.getMessage());
        } catch (Exception e) {
            showWarn("Update failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteHotel() {
        Hotel h = hotelTable.getSelectionModel().getSelectedItem();
        if (h == null) { showWarn("Select a hotel to delete."); return; }
        if (!confirmAction("Hotel \"" + h.getName() + "\" will be deleted. Do you confirm?")) return;
        try {
            if (hotelService.deleteHotel(h.getId())) {
                showInfo("Hotel deleted.");
                handleClear();
                loadHotels();
            }
        } catch (Exception e) {
            showWarn("Delete failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        selectedHotel = null;
        selectedImageFile = null;
        hotelTable.getSelectionModel().clearSelection();
        hotelNameField.clear();
        addressArea.clear();
        phoneField.clear();
        emailField.clear();
        countryCombo.setValue(null);
        cityField.clear();
        if (hotelImagePreview != null) {
            hotelImagePreview.setImage(null);
        }
    }

    @FXML
    private void handleSelectPhoto() {
        Window owner = hotelNameField.getScene() != null ? hotelNameField.getScene().getWindow() : null;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select hotel photo");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );
        File f = chooser.showOpenDialog(owner);
        if (f != null && f.isFile()) {
            selectedImageFile = f;
            if (hotelImagePreview != null) {
                hotelImagePreview.setImage(HotelImageStorage.loadFxImage(f.getAbsolutePath()));
            }
        }
    }

    @FXML
    private void handleClearPhoto() {
        selectedImageFile = null;
        if (selectedHotel != null && hotelImagePreview != null) {
            hotelImagePreview.setImage(HotelImageStorage.loadFxImage(selectedHotel.getImagePath()));
            return;
        }
        if (hotelImagePreview != null) {
            hotelImagePreview.setImage(null);
        }
    }

    @FXML
    private void handleBack() {
        MainApp.navigateTo("admin-dashboard.fxml");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }

    @FXML
    private void handleManageRooms() {
        Hotel h = hotelTable.getSelectionModel().getSelectedItem();
        if (h == null) {
            showWarn("Please select a hotel first.");
            return;
        }
        AdminRoomController.openForHotel(h);
    }

    private Hotel buildFromForm() {
        String name = hotelNameField.getText() != null ? hotelNameField.getText().trim() : "";
        String addr = addressArea.getText() != null ? addressArea.getText().trim() : "";
        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        return new Hotel(name, 0, 0, addr, phone, email);
    }

    private void saveSelectedPhotoIfAny(int hotelId, Hotel createdHotel) throws IOException {
        if (selectedImageFile == null) return;
        String path = HotelImageStorage.copyToHotelFile(hotelId, selectedImageFile);
        createdHotel.setId(hotelId);
        createdHotel.setImagePath(path);
        hotelService.updateHotel(createdHotel, countryCombo.getValue(), cityField.getText());
    }

    private boolean confirmAction(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showWarn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }
}

