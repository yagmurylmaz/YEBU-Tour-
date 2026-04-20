package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.*;
import com.hotel.service.ExtraServiceCatalogService;
import com.hotel.service.ReservationService;
import com.hotel.service.RoomImageService;
import com.hotel.service.RoomService;
import com.hotel.util.RoomImageStorage;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ReservationController {
    @FXML private ImageView roomImageView;
    @FXML private HBox roomThumbBar;
    @FXML private Button btnPrevImage;
    @FXML private Button btnNextImage;
    @FXML private Label labelRoomNo;
    @FXML private Label labelRoomType;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Label labelCheckIn;
    @FXML private Label labelCheckOut;
    @FXML private Label labelNights;
    @FXML private Label labelRoomPrice;
    @FXML private Label labelTotalPrice;
    @FXML private CheckBox cbBreakfast;
    @FXML private CheckBox cbGym;
    @FXML private CheckBox cbPool;
    @FXML private CheckBox cbParking;
    @FXML private Label labelBreakfastPrice;
    @FXML private Label labelGymPrice;
    @FXML private Label labelPoolPrice;
    @FXML private Label labelParkingPrice;
    @FXML private Label labelBreakfastDesc;
    @FXML private Label labelGymDesc;
    @FXML private Label labelPoolDesc;
    @FXML private Label labelParkingDesc;

    private final ReservationService reservationService = new ReservationService();
    private final RoomImageService roomImageService = new RoomImageService();
    private final ExtraServiceCatalogService extraServiceCatalogService = new ExtraServiceCatalogService();
    private final RoomService roomService = new RoomService();

    private Room      room;
    private LocalDate checkIn;
    private LocalDate checkOut;

    private final List<String> roomImagePaths = new ArrayList<>();
    private int currentImageIndex = 0;
    private final List<ExtraServiceDefinition> activeServiceDefs = new ArrayList<>();
    private Set<LocalDate> availableDates = Set.of();

    @FXML
    private void initialize() {
        room     = RoomSearchController.getSelectedRoom();
        checkIn  = RoomSearchController.getSelectedCheckIn();
        checkOut = RoomSearchController.getSelectedCheckOut();

        if (room == null) {
            MainApp.navigateTo("room-search.fxml");
            return;
        }
        availableDates = roomService.getRoomAvailableDates(room.getId());
        setupCalendarPickers();
        loadRoomGallery();

        labelRoomNo.setText(room.getRoomNumber());
        labelRoomType.setText(room.getRoomTypeDisplay());
        labelCheckIn.setText(checkIn.toString());
        labelCheckOut.setText(checkOut.toString());
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        labelNights.setText(nights + " night(s)");
        labelRoomPrice.setText(room.getFormattedPrice() + " × " + nights + " night(s)");
        loadServiceDefinitions();
        cbBreakfast.setOnAction(e -> updateTotal());
        cbGym.setOnAction(e -> updateTotal());
        cbPool.setOnAction(e -> updateTotal());
        cbParking.setOnAction(e -> updateTotal());

        updateTotal();
    }

    private void loadRoomGallery() {
        roomImagePaths.clear();
        currentImageIndex = 0;

        var images = roomImageService.getImagesForRoom(room.getId());
        for (var ri : images) {
            if (ri.getImagePath() != null && !ri.getImagePath().isBlank()) {
                roomImagePaths.add(ri.getImagePath());
            }
        }
        if (roomImagePaths.isEmpty() && room.getImagePath() != null && !room.getImagePath().isBlank()) {
            roomImagePaths.add(room.getImagePath());
        }

        if (roomThumbBar != null) {
            roomThumbBar.getChildren().clear();
            for (int i = 0; i < roomImagePaths.size(); i++) {
                final int idx = i;
                Image img = RoomImageStorage.loadFxImage(roomImagePaths.get(i));
                ImageView thumb = new ImageView(img);
                thumb.setFitWidth(72);
                thumb.setFitHeight(54);
                thumb.setPreserveRatio(true);
                thumb.getStyleClass().add("room-thumb");
                thumb.setOnMouseClicked(e -> {
                    currentImageIndex = idx;
                    updateMainImage();
                });
                roomThumbBar.getChildren().add(thumb);
            }
        }

        updateMainImage();
    }

    private void updateMainImage() {
        Image img = null;
        if (!roomImagePaths.isEmpty() && currentImageIndex >= 0 && currentImageIndex < roomImagePaths.size()) {
            img = RoomImageStorage.loadFxImage(roomImagePaths.get(currentImageIndex));
        }

        roomImageView.setImage(img);
        roomImageView.setVisible(img != null);
        roomImageView.setManaged(img != null);

        boolean hasMultiple = roomImagePaths.size() > 1;
        if (btnPrevImage != null) btnPrevImage.setDisable(!hasMultiple);
        if (btnNextImage != null) btnNextImage.setDisable(!hasMultiple);
    }

    private void updateTotal() {
        if (room == null || checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            labelTotalPrice.setText("-");
            return;
        }
        double total = reservationService.calculateTotalPrice(
            room, checkIn, checkOut, getSelectedServices()
        );
        labelTotalPrice.setText(String.format("₺%.2f", total));
    }

    private List<Service> getSelectedServices() {
        List<Service> services = new ArrayList<>();
        addIfSelected(services, cbBreakfast, "BREAKFAST");
        addIfSelected(services, cbGym, "GYM");
        addIfSelected(services, cbPool, "POOL");
        addIfSelected(services, cbParking, "PARKING");
        return services;
    }

    @FXML
    private void handlePrevImage() {
        if (roomImagePaths.size() <= 1) return;
        currentImageIndex = (currentImageIndex - 1 + roomImagePaths.size()) % roomImagePaths.size();
        updateMainImage();
    }

    @FXML
    private void handleNextImage() {
        if (roomImagePaths.size() <= 1) return;
        currentImageIndex = (currentImageIndex + 1) % roomImagePaths.size();
        updateMainImage();
    }

    @FXML
    private void handleConfirmReservation() {
        try {
            reservationService.validateDates(checkIn, checkOut);
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Date Error", e.getMessage());
            return;
        }
        if (!roomService.isRoomAvailableForRange(room.getId(), checkIn, checkOut)) {
            showAlert(Alert.AlertType.WARNING, "Not Available",
                "Selected room is not available for the chosen date range.");
            return;
        }
        int customerId = SessionManager.getInstance().getLoggedInUser().getId();
        List<Service> services = getSelectedServices();

        double total = reservationService.calculateTotalPrice(room, checkIn, checkOut, services);

        Reservation reservation = new Reservation(
            customerId, room.getId(), checkIn, checkOut, total
        );

        int newId = reservationService.createReservation(reservation, services);

        if (newId > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Reservation Success");
            alert.setHeaderText("Your reservation has been received! 🎉");
            alert.setContentText(
                "Reservation No: #" + newId + "\n" +
                "Room: " + room.getRoomNumber() + " (" + room.getRoomTypeDisplay() + ")\n" +
                "Check-in: " + checkIn + "  |  Check-out: " + checkOut + "\n" +
                "Total Amount: ₺" + String.format("%.2f", total) + "\n\n" +
                "Your reservation is pending approval."
            );
            alert.showAndWait();
            MainApp.navigateTo("my-reservations.fxml");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Reservation could not be created.");
            alert.setContentText("Please try again or contact support.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleBack() {
        MainApp.navigateTo("room-search.fxml");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }

    private void loadServiceDefinitions() {
        activeServiceDefs.clear();
        activeServiceDefs.addAll(extraServiceCatalogService.getActiveServices());

        bindServiceUi("BREAKFAST", labelBreakfastPrice, labelBreakfastDesc);
        bindServiceUi("GYM", labelGymPrice, labelGymDesc);
        bindServiceUi("POOL", labelPoolPrice, labelPoolDesc);
        bindServiceUi("PARKING", labelParkingPrice, labelParkingDesc);
    }

    private void bindServiceUi(String code, Label priceLabel, Label descLabel) {
        ExtraServiceDefinition def = findServiceDef(code);
        if (def == null) return;
        String unit = "PER_STAY".equalsIgnoreCase(def.getBillingType()) ? "per stay" : "per night(s)";
        if (priceLabel != null) priceLabel.setText("+ ₺" + def.getPrice() + " / " + unit);
        if (descLabel != null) descLabel.setText(def.getDescription());
    }

    private void addIfSelected(List<Service> out, CheckBox cb, String code) {
        if (cb == null || !cb.isSelected()) return;
        ExtraServiceDefinition def = findServiceDef(code);
        if (def != null) {
            out.add(new SelectedExtraService(def.getCode(), def.getName(), def.getPrice(), def.getBillingType(), 1));
            return;
        }
        switch (code) {
            case "BREAKFAST" -> out.add(new BreakfastService());
            case "GYM" -> out.add(new GymService());
            case "POOL" -> out.add(new PoolService());
            case "PARKING" -> out.add(new ParkingService());
            default -> { }
        }
    }

    private ExtraServiceDefinition findServiceDef(String code) {
        return activeServiceDefs.stream()
            .filter(d -> code.equalsIgnoreCase(d.getCode()))
            .findFirst()
            .orElse(null);
    }

    private void setupCalendarPickers() {
        if (checkInPicker == null || checkOutPicker == null) return;
        checkInPicker.setValue(checkIn);
        checkOutPicker.setValue(checkOut);

        checkInPicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) return;
                boolean notInAvailability = !availableDates.isEmpty() && !availableDates.contains(item);
                setDisable(item.isBefore(LocalDate.now()) || notInAvailability);
            }
        });

        checkOutPicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) return;
                LocalDate selectedCheckIn = checkInPicker.getValue();
                boolean beforeOrSameCheckIn = selectedCheckIn != null && !item.isAfter(selectedCheckIn);
                boolean outsideAvailability = selectedCheckIn != null && !isRangeInsideAvailability(selectedCheckIn, item);
                setDisable(item.isBefore(LocalDate.now()) || beforeOrSameCheckIn || outsideAvailability);
            }
        });

        checkInPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            if (checkOutPicker.getValue() == null || !checkOutPicker.getValue().isAfter(newVal)
                || !isRangeInsideAvailability(newVal, checkOutPicker.getValue())) {
                checkOutPicker.setValue(newVal.plusDays(1));
            }
            applySelectedDates();
        });
        checkOutPicker.valueProperty().addListener((obs, oldVal, newVal) -> applySelectedDates());
    }

    private void applySelectedDates() {
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        if (in == null || out == null) return;
        if (!out.isAfter(in)) return;
        if (!isRangeInsideAvailability(in, out)) return;
        checkIn = in;
        checkOut = out;
        labelCheckIn.setText(checkIn.toString());
        labelCheckOut.setText(checkOut.toString());
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        labelNights.setText(nights + " night(s)");
        labelRoomPrice.setText(room.getFormattedPrice() + " × " + nights + " night(s)");
        updateTotal();
    }

    private boolean isRangeInsideAvailability(LocalDate start, LocalDate end) {
        if (availableDates == null || availableDates.isEmpty()) return true;
        LocalDate day = start;
        while (day.isBefore(end)) {
            if (!availableDates.contains(day)) return false;
            day = day.plusDays(1);
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
