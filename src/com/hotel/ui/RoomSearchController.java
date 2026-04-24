package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Hotel;
import com.hotel.model.HotelReview;
import com.hotel.model.Room;
import com.hotel.service.HotelReviewService;
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
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomSearchController {

    @FXML private DatePicker    checkInDatePicker;
    @FXML private DatePicker    checkOutDatePicker;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private ComboBox<String> countryCombo;
    @FXML private ComboBox<String> cityCombo;
    @FXML private ComboBox<Hotel> hotelCombo;
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
    @FXML private Label hotelRatingLabel;
    @FXML private ListView<String> hotelReviewListView;

    private final RoomService        roomService        = new RoomService();
    private final HotelService       hotelService       = new HotelService();
    private final HotelReviewService hotelReviewService = new HotelReviewService();
    private final ReservationService reservationService = new ReservationService();
    private final RoomImageService   roomImageService   = new RoomImageService();
    private final Map<Integer, String> coverPathCache = new HashMap<>();
    private final List<Hotel> allHotels = new java.util.ArrayList<>();
    private final Map<Integer, String> hotelCoverCache = new HashMap<>();

    private static Room selectedRoom;
    private static LocalDate selectedCheckIn;
    private static LocalDate selectedCheckOut;
    private static Integer preferredHotelId;

    @FXML
    private void initialize() {
        roomTypeCombo.setItems(FXCollections.observableArrayList(
            "ALL", "SINGLE", "DOUBLE", "SUITE", "DELUXE"
        ));
        roomTypeCombo.setValue("ALL");
        setupLocationFilters();
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
                var img = RoomImageStorage.loadFxImage(resolveHotelImagePath(room));
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
                clearHotelReviews();
            } else {
                loadSelectedGallery(room);
                loadHotelReviews(room);
            }
        });
        setupReviewListView();
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
        hotelCoverCache.clear();
        clearSelectedGallery();
        clearHotelReviews();

        if (rooms.isEmpty()) {
            statusLabel.setText("No available rooms for the selected date range.");
        } else {
            statusLabel.setText(rooms.size() + " available rooms listed. "
                + "Select a room to proceed with reservation.");
            roomTable.getSelectionModel().selectFirst();
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
    public static void setPreferredHotelId(Integer hotelId) { preferredHotelId = hotelId; }

    private void clearSelectedGallery() {
        if (selectedRoomImageView != null) {
            selectedRoomImageView.setImage(null);
        }
        if (selectedRoomThumbBar != null) {
            selectedRoomThumbBar.getChildren().clear();
        }
    }

    private void loadSelectedGallery(Room room) {
        if (selectedRoomThumbBar == null || selectedRoomImageView == null) return;
        selectedRoomThumbBar.getChildren().clear();
        if (room == null) return;

        int roomId = room.getId();
        String hotelImagePath = resolveHotelImagePath(room);
        String fallbackCoverPath = resolveCoverImagePath(room);

        var images = roomImageService.getImagesForRoom(roomId);
        String first = !images.isEmpty() ? images.get(0).getImagePath() : fallbackCoverPath;
        var firstImg = RoomImageStorage.loadFxImage(first);
        selectedRoomImageView.setImage(firstImg);

        // Thumbnail bar: always show only hotel photo.
        if (hotelImagePath != null && !hotelImagePath.isBlank()) {
            var hotelThumb = RoomImageStorage.loadFxImage(hotelImagePath);
            if (hotelThumb != null) {
                ImageView iv = new ImageView(hotelThumb);
                iv.setFitWidth(72);
                iv.setFitHeight(54);
                iv.setPreserveRatio(true);
                iv.setOnMouseClicked(e -> selectedRoomImageView.setImage(RoomImageStorage.loadFxImage(hotelImagePath)));
                selectedRoomThumbBar.getChildren().add(iv);
            }
        }

        // Room photos are kept in the main "Photos" display (selectedRoomImageView).
        for (var ri : images) {
            if (firstImg == null) {
                var roomImg = RoomImageStorage.loadFxImage(ri.getImagePath());
                if (roomImg != null) {
                    selectedRoomImageView.setImage(roomImg);
                    firstImg = roomImg;
                    break;
                }
            }
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
        String hotelPath = room.getHotelImagePath();
        if (hotelPath != null && !hotelPath.isBlank()) {
            coverPathCache.put(room.getId(), hotelPath);
            return hotelPath;
        }
        Integer hid = room.getHotelId();
        if (hid != null && hid > 0) {
            String cachedHotelPath = hotelCoverCache.get(hid);
            if (cachedHotelPath != null && !cachedHotelPath.isBlank()) {
                coverPathCache.put(room.getId(), cachedHotelPath);
                return cachedHotelPath;
            }
            String lookedUp = allHotels.stream()
                .filter(h -> h.getId() == hid)
                .map(Hotel::getImagePath)
                .filter(p -> p != null && !p.isBlank())
                .findFirst()
                .orElse(null);
            if (lookedUp != null) {
                hotelCoverCache.put(hid, lookedUp);
                coverPathCache.put(room.getId(), lookedUp);
                return lookedUp;
            }
        }
        return null;
    }

    private String resolveHotelImagePath(Room room) {
        if (room == null) return null;
        String hotelPath = room.getHotelImagePath();
        if (hotelPath != null && !hotelPath.isBlank()) return hotelPath;
        Integer hid = room.getHotelId();
        if (hid != null && hid > 0) {
            String cachedHotelPath = hotelCoverCache.get(hid);
            if (cachedHotelPath != null && !cachedHotelPath.isBlank()) return cachedHotelPath;
            String lookedUp = allHotels.stream()
                .filter(h -> h.getId() == hid)
                .map(Hotel::getImagePath)
                .filter(p -> p != null && !p.isBlank())
                .findFirst()
                .orElse(null);
            if (lookedUp != null) {
                hotelCoverCache.put(hid, lookedUp);
                return lookedUp;
            }
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

    private void clearHotelReviews() {
        if (hotelRatingLabel != null) hotelRatingLabel.setText("No rating yet");
        if (hotelReviewListView != null) hotelReviewListView.getItems().clear();
    }

    private void loadHotelReviews(Room room) {
        if (room == null || room.getHotelId() == null || room.getHotelId() <= 0) {
            clearHotelReviews();
            return;
        }
        List<HotelReview> reviews = hotelReviewService.getReviewsForHotel(room.getHotelId());
        double avg = hotelReviewService.getAverageStarsForHotel(room.getHotelId());
        if (hotelRatingLabel != null) {
            if (reviews.isEmpty()) {
                hotelRatingLabel.setText("No rating yet");
            } else {
                hotelRatingLabel.setText(String.format("Average: %.1f / 5 (%d review)", avg, reviews.size()));
            }
        }
        if (hotelReviewListView != null) {
            List<String> rows = reviews.stream()
                .map(r -> "★".repeat(Math.max(1, r.getStars())) + " - " + r.getCustomerName() + ": " + r.getComment())
                .toList();
            hotelReviewListView.setItems(FXCollections.observableArrayList(rows));
        }
    }

    private void setupReviewListView() {
        if (hotelReviewListView == null) return;
        hotelReviewListView.setCellFactory(list -> new ListCell<>() {
            private final Text text = new Text();
            {
                text.wrappingWidthProperty().bind(list.widthProperty().subtract(28));
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    return;
                }
                text.setText(item);
                setGraphic(text);
            }
        });
    }

    private Integer resolveSelectedHotelId() {
        if (hotelCombo == null) return null;
        Hotel selected = hotelCombo.getValue();
        if (selected == null || selected.getId() <= 0) return null;
        return selected.getId();
    }

    private void setupLocationFilters() {
        allHotels.clear();
        allHotels.addAll(hotelService.getAllHotels());

        if (countryCombo == null || cityCombo == null || hotelCombo == null) return;

        List<String> countries = allHotels.stream()
            .map(Hotel::getCountryName)
            .filter(s -> s != null && !s.isBlank())
            .distinct()
            .sorted()
            .toList();
        var countryItems = FXCollections.observableArrayList(countries);
        countryItems.add(0, "ALL");
        countryCombo.setItems(countryItems);
        countryCombo.setValue("ALL");

        countryCombo.valueProperty().addListener((obs, old, val) -> refreshCityCombo());
        cityCombo.valueProperty().addListener((obs, old, val) -> refreshHotelCombo());

        refreshCityCombo();
        applyPreferredHotelSelection();
    }

    private void refreshCityCombo() {
        String selectedCountry = countryCombo.getValue();
        List<String> cities = allHotels.stream()
            .filter(h -> "ALL".equals(selectedCountry) || h.getCountryName().equals(selectedCountry))
            .map(Hotel::getCityName)
            .filter(s -> s != null && !s.isBlank())
            .distinct()
            .sorted()
            .toList();
        var cityItems = FXCollections.observableArrayList(cities);
        cityItems.add(0, "ALL");
        cityCombo.setItems(cityItems);
        cityCombo.setValue("ALL");
        refreshHotelCombo();
    }

    private void refreshHotelCombo() {
        String selectedCountry = countryCombo.getValue();
        String selectedCity = cityCombo.getValue();
        List<Hotel> hotels = allHotels.stream()
            .filter(h -> "ALL".equals(selectedCountry) || h.getCountryName().equals(selectedCountry))
            .filter(h -> "ALL".equals(selectedCity) || h.getCityName().equals(selectedCity))
            .toList();
        Hotel allOption = new Hotel();
        allOption.setId(0);
        allOption.setName("ALL");
        var hotelItems = FXCollections.observableArrayList(hotels);
        hotelItems.add(0, allOption);
        hotelCombo.setItems(hotelItems);
        hotelCombo.setValue(allOption);
    }

    private void applyPreferredHotelSelection() {
        if (preferredHotelId == null || preferredHotelId <= 0 || hotelCombo == null) return;
        Hotel preferred = allHotels.stream()
            .filter(h -> h.getId() == preferredHotelId)
            .findFirst()
            .orElse(null);
        if (preferred == null) {
            preferredHotelId = null;
            return;
        }

        if (countryCombo != null) {
            String country = preferred.getCountryName();
            if (country != null && !country.isBlank()) {
                countryCombo.setValue(country);
            }
        }
        if (cityCombo != null) {
            String city = preferred.getCityName();
            if (city != null && !city.isBlank()) {
                cityCombo.setValue(city);
            }
        }

        refreshHotelCombo();
        for (Hotel h : hotelCombo.getItems()) {
            if (h != null && h.getId() == preferredHotelId) {
                hotelCombo.setValue(h);
                break;
            }
        }
        handleSearch();
        preferredHotelId = null;
    }
}
