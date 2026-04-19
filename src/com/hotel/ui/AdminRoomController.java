package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Room;
import com.hotel.model.Room.RoomType;
import com.hotel.service.RoomService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

public class AdminRoomController {
    @FXML private TextField        roomNumberField;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private TextField        priceField;
    @FXML private TextField        capacityField;
    @FXML private TextField        descriptionField;
    @FXML private CheckBox         availableCheck;
    @FXML private TableView<Room>           roomTable;
    @FXML private TableColumn<Room, String> colNo;
    @FXML private TableColumn<Room, String> colType;
    @FXML private TableColumn<Room, String> colPrice;
    @FXML private TableColumn<Room, String> colCapacity;
    @FXML private TableColumn<Room, String> colDesc;
    @FXML private TableColumn<Room, String> colAvailable;

    @FXML private Label statusLabel;

    private final RoomService roomService = new RoomService();
    private Room selectedRoom = null;

    @FXML
    private void initialize() {
        roomTypeCombo.setItems(FXCollections.observableArrayList(
            "SINGLE", "DOUBLE", "SUITE", "DELUXE"
        ));
        availableCheck.setSelected(true);

        setupTableColumns();
        loadRooms();
        roomTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, room) -> {
                if (room != null) populateForm(room);
            }
        );
    }

    private void setupTableColumns() {
        colNo.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoomNumber()));
        colType.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoomTypeDisplay()));
        colPrice.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getFormattedPrice()));
        colCapacity.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCapacity() + " guests"));
        colDesc.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getDescription()));
        colAvailable.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().isAvailable() ? "✔ Available" : "✘ Occupied"));
    }

    private void loadRooms() {
        List<Room> rooms = roomService.getAllRooms();
        roomTable.setItems(FXCollections.observableArrayList(rooms));
        statusLabel.setText("Listed " + rooms.size() + " room(s).");
    }

    @FXML
    private void handleAddRoom() {
        try {
            Room room = buildRoomFromForm();
            int newId = roomService.addRoom(room);
            if (newId > 0) {
                showInfo("Room added successfully. (ID: " + newId + ")");
                clearForm();
                loadRooms();
            }
        } catch (NumberFormatException e) {
            showWarn("Enter valid numeric values for price and capacity.");
        } catch (IllegalArgumentException e) {
            showWarn(e.getMessage());
        }
    }

    @FXML
    private void handleUpdateRoom() {
        if (selectedRoom == null) { showWarn("Select a room from the table to update."); return; }
        try {
            Room room = buildRoomFromForm();
            room.setId(selectedRoom.getId());
            if (roomService.updateRoom(room)) {
                showInfo("Room updated.");
                clearForm();
                loadRooms();
            }
        } catch (NumberFormatException e) {
            showWarn("Enter valid numeric values for price and capacity.");
        } catch (IllegalArgumentException e) {
            showWarn(e.getMessage());
        }
    }

    @FXML
    private void handleDeleteRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarn("Select a room from the table to delete."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Room " + selected.getRoomNumber() + " will be deleted. Do you confirm?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (roomService.deleteRoom(selected.getId())) {
                showInfo("Room deleted.");
                clearForm();
                loadRooms();
            } else {
                showWarn("Delete failed. The room may have an active reservation.");
            }
        }
    }

    @FXML
    private void handleClearForm() {
        clearForm();
    }

    @FXML private void handleBack()   { MainApp.navigateTo("admin-dashboard.fxml"); }
    @FXML private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }

    private void populateForm(Room room) {
        selectedRoom = room;
        roomNumberField.setText(room.getRoomNumber());
        roomTypeCombo.setValue(room.getRoomType().name());
        priceField.setText(String.valueOf(room.getPricePerNight()));
        capacityField.setText(String.valueOf(room.getCapacity()));
        descriptionField.setText(room.getDescription());
        availableCheck.setSelected(room.isAvailable());
    }

    private Room buildRoomFromForm() {
        String number      = roomNumberField.getText().trim();
        String typeStr     = roomTypeCombo.getValue();
        double price       = Double.parseDouble(priceField.getText().trim());
        int    capacity    = Integer.parseInt(capacityField.getText().trim());
        String description = descriptionField.getText().trim();
        boolean available  = availableCheck.isSelected();

        if (typeStr == null) throw new IllegalArgumentException("Room type must be selected.");
        RoomType type = RoomType.valueOf(typeStr);
        Room room = new Room(number, type, price, capacity, description);
        room.setAvailable(available);
        return room;
    }

    private void clearForm() {
        selectedRoom = null;
        roomNumberField.clear();
        roomTypeCombo.setValue(null);
        priceField.clear();
        capacityField.clear();
        descriptionField.clear();
        availableCheck.setSelected(true);
        roomTable.getSelectionModel().clearSelection();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showWarn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }
}
