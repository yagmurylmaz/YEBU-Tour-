package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Hotel;
import com.hotel.model.Room;
import com.hotel.service.HotelService;
import com.hotel.service.ReservationService;
import com.hotel.service.RoomImageService;
import com.hotel.service.RoomService;
import com.hotel.util.RoomImageStorage;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomSearchController {

    @FXML private DatePicker    checkInDatePicker;
    @FXML private DatePicker    checkOutDatePicker;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private ComboBox<String> hotelCombo;
    @FXML private TableView<Room>  roomTable;
    @FXML private TableColumn<Room, Room> colPhoto;
    @FXML private TableColumn<Room, String> colRoomNo;
    @FXML private TableColumn<Room, String> colHotel;
    @FXML private TableColumn<Room, String> colType;
    @FXML private TableColumn<Room, String> colPrice;
    @FXML private TableColumn<Room, String> colCapacity;
    @FXML private TableColumn<Room, String> colDescription;
    @FXML private Label statusLabel;
    @FXML private ImageView selectedRoomImageView;
    @FXML private HBox selectedRoomThumbBar;

    private final RoomService        roomService        = new RoomService();
    private final HotelService       hotelService       = new HotelService();
    private final ReservationService reservationService = new ReservationService();
    private final RoomImageService   roomImageService   = new RoomImageService();
    private final Map<Integer, String> coverPathCache = new HashMap<>();

    private static Room selectedRoom;
    private static LocalDate selectedCheckIn;
    private static LocalDate selectedCheckOut;

    @FXML
    private void initialize() {
        roomTypeCombo.setItems(FXCollections.observableArrayList(
            "ALL", "SINGLE", "DOUBLE", "SUITE", "DELUXE"
        ));
        roomTypeCombo.setValue("ALL");
        if (hotelCombo != null) {
            var hotelNames = hotelService.getAllHotels().stream()
                .map(Hotel::getName)
                .distinct()
                .sorted()
                .toList();
            var opts = FXCollections.observableArrayList(hotelNames);
            opts.add(0, "ALL");
            hotelCombo.setItems(opts);
            hotelCombo.setValue("ALL");
        }
        colPhoto.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colPhoto.setCellFactory(col -> new TableCell<Room, Room>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(72);
                iv.setFitHeight(54);
                iv.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setGraphic(null);
                    return;
                }
                var img = RoomImageStorage.loadFxImage(resolveCoverImagePath(room));
                iv.setImage(img);
                setGraphic(img != null ? iv : null);
            }
        });
        colRoomNo.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoomNumber()));
        if (colHotel != null) {
            colHotel.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getHotelName()));
        }
        colType.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoomTypeDisplay()));
        colPrice.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getFormattedPrice() + " / night(s)"));
        colCapacity.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCapacity() + " guests"));
        colDescription.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getDescription()));
        checkInDatePicker.setValue(LocalDate.now());
        checkOutDatePicker.setValue(LocalDate.now().plusDays(1));

        statusLabel.setText("Select dates and room type to search.");

        roomTable.getSelectionModel().selectedItemProperty().addListener((obs, old, room) -> {
            if (room == null) {
                clearSelectedGallery();
            } else {
                loadSelectedGallery(room.getId(), room.getImagePath());
            }
        });
    }

    @FXML
    private void handleSearch() {
        LocalDate checkIn  = checkInDatePicker.getValue();
        LocalDate checkOut = checkOutDatePicker.getValue();
        try {
            reservationService.validateDates(checkIn, checkOut);
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Date Error", e.getMessage());
            return;
        }

        String roomType = roomTypeCombo.getValue();
        Integer hotelId = resolveSelectedHotelId();
        List<Room> rooms = roomService.searchAvailableRooms(checkIn, checkOut, roomType, hotelId);

        roomTable.setItems(FXCollections.observableArrayList(rooms));
        coverPathCache.clear();
        clearSelectedGallery();

        if (rooms.isEmpty()) {
            statusLabel.setText("No available rooms for the selected date range.");
        } else {
            statusLabel.setText(rooms.size() + " available rooms listed. "
                + "Select a room to proceed with reservation.");
        }
    }

    @FXML
    private void handleMakeReservation() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Room Not Selected",
                "Please select a room you want to reserve.");
            return;
        }
        selectedRoom      = selected;
        selectedCheckIn   = checkInDatePicker.getValue();
        selectedCheckOut  = checkOutDatePicker.getValue();

        MainApp.navigateTo("reservation.fxml");
    }

    @FXML
    private void handleBack() {
        MainApp.navigateTo("customer-dashboard.fxml");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }

    public static Room getSelectedRoom()         { return selectedRoom; }
    public static LocalDate getSelectedCheckIn() { return selectedCheckIn; }
    public static LocalDate getSelectedCheckOut(){ return selectedCheckOut; }

    private void clearSelectedGallery() {
        if (selectedRoomImageView != null) {
            selectedRoomImageView.setImage(null);
        }
        if (selectedRoomThumbBar != null) {
            selectedRoomThumbBar.getChildren().clear();
        }
    }

    private void loadSelectedGallery(int roomId, String fallbackCoverPath) {
        if (selectedRoomThumbBar == null || selectedRoomImageView == null) return;
        selectedRoomThumbBar.getChildren().clear();

        var images = roomImageService.getImagesForRoom(roomId);
        String first = !images.isEmpty() ? images.get(0).getImagePath() : fallbackCoverPath;
        selectedRoomImageView.setImage(RoomImageStorage.loadFxImage(first));

        for (var ri : images) {
            ImageView iv = new ImageView(RoomImageStorage.loadFxImage(ri.getImagePath()));
            iv.setFitWidth(72);
            iv.setFitHeight(54);
            iv.setPreserveRatio(true);
            iv.setOnMouseClicked(e -> selectedRoomImageView.setImage(RoomImageStorage.loadFxImage(ri.getImagePath())));
            selectedRoomThumbBar.getChildren().add(iv);
        }
    }

    private String resolveCoverImagePath(Room room) {
        if (room == null) return null;
        String roomPath = room.getImagePath();
        if (roomPath != null && !roomPath.isBlank()) return roomPath;

        String cached = coverPathCache.get(room.getId());
        if (cached != null) return cached;

        var images = roomImageService.getImagesForRoom(room.getId());
        String first = images.isEmpty() ? null : images.get(0).getImagePath();
        if (first != null && !first.isBlank()) {
            coverPathCache.put(room.getId(), first);
            return first;
        }
        return null;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Integer resolveSelectedHotelId() {
        if (hotelCombo == null) return null;
        String selected = hotelCombo.getValue();
        if (selected == null || selected.equals("ALL")) return null;
        return hotelService.getAllHotels().stream()
            .filter(h -> selected.equals(h.getName()))
            .map(Hotel::getId)
            .findFirst()
            .orElse(null);
    }
}
