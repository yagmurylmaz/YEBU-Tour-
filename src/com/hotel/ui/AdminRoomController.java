package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Hotel;
import com.hotel.model.Room;
import com.hotel.model.Room.RoomType;
import com.hotel.model.RoomImage;
import com.hotel.service.HotelService;
import com.hotel.service.RoomImageService;
import com.hotel.service.RoomService;
import com.hotel.util.RoomImageStorage;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminRoomController {
    private static Integer scopedHotelId;
    private static String scopedHotelName;

    @FXML private TextField        roomNumberField;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private TextField        priceField;
    @FXML private TextField        capacityField;
    @FXML private TextField        descriptionField;
    @FXML private ImageView        roomImagePreview;
    @FXML private HBox             photoThumbBar;
    @FXML private CheckBox         availableCheck;
    @FXML private TableView<Room>           roomTable;
    @FXML private TableColumn<Room, String> colHotel;
    @FXML private TableColumn<Room, String> colNo;
    @FXML private TableColumn<Room, String> colType;
    @FXML private TableColumn<Room, String> colPrice;
    @FXML private TableColumn<Room, String> colCapacity;
    @FXML private TableColumn<Room, String> colDesc;
    @FXML private TableColumn<Room, String> colAvailable;

    @FXML private Label statusLabel;
    @FXML private Label selectedHotelLabel;

    private final RoomService roomService = new RoomService();
    private final RoomImageService roomImageService = new RoomImageService();
    private final HotelService hotelService = new HotelService();
    private Room selectedRoom = null;
    private final List<File> pendingImageFiles = new ArrayList<>();
    private final List<RoomImage> currentImages = new ArrayList<>();
    private RoomImage selectedImage;

    @FXML
    private void initialize() {
        if (!SessionManager.getInstance().ensureAdminAccess()) return;
        if (scopedHotelId == null || scopedHotelId <= 0) {
            showWarn("Please select a hotel first.");
            MainApp.navigateTo("admin-hotels.fxml");
            return;
        }
        roomTypeCombo.setItems(FXCollections.observableArrayList(
            "SINGLE", "DOUBLE", "SUITE", "DELUXE"
        ));
        availableCheck.setSelected(true);
        if (selectedHotelLabel != null) {
            selectedHotelLabel.setText("Selected hotel: " + (scopedHotelName == null ? "#" + scopedHotelId : scopedHotelName));
        }

        setupTableColumns();
        loadRooms();
        roomTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, room) -> {
                if (room != null) populateForm(room);
            }
        );
    }

    private void setupTableColumns() {
        colHotel.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getHotelName()));
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
        List<Room> rooms = roomService.getRoomsByHotelId(scopedHotelId);
        roomTable.setItems(FXCollections.observableArrayList(rooms));
        statusLabel.setText("Listed " + rooms.size() + " room(s) for selected hotel.");
    }

    @FXML
    private void handleAddPhotos() {
        Window owner = roomNumberField.getScene() != null ? roomNumberField.getScene().getWindow() : null;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select room photos");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );
        List<File> files = chooser.showOpenMultipleDialog(owner);
        if (files != null && !files.isEmpty()) {
            pendingImageFiles.addAll(files.stream().filter(f -> f != null && f.isFile()).toList());
            // show last selected as preview
            File last = pendingImageFiles.get(pendingImageFiles.size() - 1);
            roomImagePreview.setImage(RoomImageStorage.loadFxImage(last.getAbsolutePath()));
            renderThumbBar();
        }
    }

    @FXML
    private void handleRemoveSelectedPhoto() {
        if (selectedImage != null) {
            boolean removed = roomImageService.deleteImage(selectedImage.getId());
            if (removed) {
                RoomImageStorage.deleteFileIfExists(selectedImage.getImagePath());
                loadImagesForSelectedRoom();
            }
            return;
        }
        // if selecting a not-yet-saved pending file: remove last
        if (!pendingImageFiles.isEmpty()) {
            pendingImageFiles.remove(pendingImageFiles.size() - 1);
            renderThumbBar();
        }
    }

    @FXML
    private void handleAddRoom() {
        try {
            Room room = buildRoomFromForm();
            room.setHotelId(scopedHotelId);
            int newId = roomService.addRoom(room);
            if (newId > 0) {
                boolean imagesOk = true;
                for (File f : pendingImageFiles) {
                    try {
                        String path = RoomImageStorage.copyToRoomFile(newId, f);
                        roomImageService.addImage(newId, path);
                    } catch (IOException e) {
                        imagesOk = false;
                        showWarn("Room #" + newId + " was added but an image could not be saved: " + e.getMessage());
                        break;
                    }
                }
                if (imagesOk) showInfo("Room added successfully. (ID: " + newId + ")");
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
            room.setImagePath(selectedRoom.getImagePath()); // cover stays unless explicitly changed later
            room.setHotelId(scopedHotelId);

            if (roomService.updateRoom(room)) {
                for (File f : pendingImageFiles) {
                    try {
                        String path = RoomImageStorage.copyToRoomFile(selectedRoom.getId(), f);
                        roomImageService.addImage(selectedRoom.getId(), path);
                    } catch (IOException e) {
                        showWarn("An image could not be added: " + e.getMessage());
                        break;
                    }
                }
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
            String imagePath = selected.getImagePath();
            if (roomService.deleteRoom(selected.getId())) {
                RoomImageStorage.deleteFileIfExists(imagePath);
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

    @FXML private void handleBack()   { MainApp.navigateTo("admin-hotels.fxml"); }
    @FXML private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }

    private void populateForm(Room room) {
        selectedRoom = room;
        pendingImageFiles.clear();
        selectedImage = null;
        roomNumberField.setText(room.getRoomNumber());
        roomTypeCombo.setValue(room.getRoomType().name());
        priceField.setText(String.valueOf(room.getPricePerNight()));
        capacityField.setText(String.valueOf(room.getCapacity()));
        descriptionField.setText(room.getDescription());
        availableCheck.setSelected(room.isAvailable());
        loadImagesForSelectedRoom();
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
        pendingImageFiles.clear();
        currentImages.clear();
        selectedImage = null;
        roomNumberField.clear();
        roomTypeCombo.setValue(null);
        priceField.clear();
        capacityField.clear();
        descriptionField.clear();
        roomImagePreview.setImage(null);
        if (photoThumbBar != null) photoThumbBar.getChildren().clear();
        availableCheck.setSelected(true);
        roomTable.getSelectionModel().clearSelection();
    }

    public static void openForHotel(Hotel hotel) {
        if (hotel == null) return;
        scopedHotelId = hotel.getId();
        scopedHotelName = hotel.getName();
        MainApp.navigateTo("admin-rooms.fxml");
    }

    private void loadImagesForSelectedRoom() {
        currentImages.clear();
        if (selectedRoom != null) {
            currentImages.addAll(roomImageService.getImagesForRoom(selectedRoom.getId()));
        }
        // Prefer first image from gallery; fallback to cover path if exists
        String previewPath = !currentImages.isEmpty()
            ? currentImages.get(0).getImagePath()
            : (selectedRoom != null ? selectedRoom.getImagePath() : null);
        roomImagePreview.setImage(RoomImageStorage.loadFxImage(previewPath));
        renderThumbBar();
    }

    private void renderThumbBar() {
        if (photoThumbBar == null) return;
        photoThumbBar.getChildren().clear();
        selectedImage = null;

        for (RoomImage ri : currentImages) {
            ImageView iv = new ImageView(RoomImageStorage.loadFxImage(ri.getImagePath()));
            iv.setFitWidth(72);
            iv.setFitHeight(54);
            iv.setPreserveRatio(true);
            iv.setOnMouseClicked(e -> {
                selectedImage = ri;
                roomImagePreview.setImage(RoomImageStorage.loadFxImage(ri.getImagePath()));
            });
            photoThumbBar.getChildren().add(iv);
        }

        for (File f : pendingImageFiles) {
            ImageView iv = new ImageView(RoomImageStorage.loadFxImage(f.getAbsolutePath()));
            iv.setFitWidth(72);
            iv.setFitHeight(54);
            iv.setPreserveRatio(true);
            iv.setOpacity(0.75);
            iv.setOnMouseClicked(e -> roomImagePreview.setImage(RoomImageStorage.loadFxImage(f.getAbsolutePath())));
            photoThumbBar.getChildren().add(iv);
        }
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showWarn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }
}
