package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Hotel;
import com.hotel.model.HotelReview;
import com.hotel.model.Room;
import com.hotel.model.User;
import com.hotel.service.HotelReviewService;
import com.hotel.service.HotelService;
import com.hotel.service.RoomImageService;
import com.hotel.service.RoomService;
import com.hotel.util.ImageStorageService;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableNumberValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.HPos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CustomerDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label supportStatusLabel;
    @FXML private GridPane hotelCardContainer;

    private final HotelService hotelService = new HotelService();
    private final HotelReviewService hotelReviewService = new HotelReviewService();
    private final RoomService roomService = new RoomService();
    private final RoomImageService roomImageService = new RoomImageService();
    private List<Hotel> hotels = List.of();

    @FXML
    private void initialize() {
        User user = SessionManager.getInstance().getLoggedInUser();
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getFullName() + "!");
        }
        hotels = hotelService.getAllHotels();
        setupHotelGridColumns();
        renderHotelCards();
    }

    /** Exactly three hotel cards per row; additional rows stack below. */
    private void setupHotelGridColumns() {
        if (hotelCardContainer == null) return;
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(100.0 / 3.0);
        c.setHgrow(Priority.ALWAYS);
        c.setMinWidth(0);
        hotelCardContainer.getColumnConstraints().setAll(c, c, c);
    }

    @FXML
    private void handleMyReservations() {
        MainApp.navigateTo("my-reservations.fxml");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }

    @FXML
    private void handleSupportEmail() {
        String supportMail = "yebutour@gmail.com";
        String mailto = "mailto:" + supportMail + "?subject=YEBU%20Support%20Request";
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
                Desktop.getDesktop().mail(URI.create(mailto));
                if (supportStatusLabel != null) {
                    supportStatusLabel.setText("Mail app opened.");
                }
                return;
            }
            if (supportStatusLabel != null) {
                supportStatusLabel.setText("Mail app unavailable. Contact: " + supportMail);
            }
        } catch (Exception e) {
            if (supportStatusLabel != null) {
                supportStatusLabel.setText("Could not open mail app. Contact: " + supportMail);
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Support");
            alert.setHeaderText("Contact Support");
            alert.setContentText("Please send an email to: " + supportMail);
            alert.showAndWait();
        }
    }

    @FXML
    private void handleAboutUs() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Us");
        alert.setHeaderText("About YEBU Tour");
        alert.setContentText(
            "YEBU Tour is a modern hotel discovery and reservation platform.\n\n"
                + "We focus on simple booking flows, transparent hotel information,\n"
                + "and a better guest experience.\n\n"
                + "Team: Yiğitcan Yıldız, Mehmet Efe İnan, Beyda Taşan, Yağmur Yılmaz, Ulaş Deniz Ay"
        );
        alert.showAndWait();
    }

    @FXML
    private void handleContactUs() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Contact Us");
        alert.setHeaderText("Contact YEBU Support");
        alert.setContentText(
            "Email: yebutour@gmail.com\n"
                + "For quick support, use the 'Send Email' button in the footer."
        );
        alert.showAndWait();
    }

    private void renderHotelCards() {
        if (hotelCardContainer == null) return;
        hotelCardContainer.getChildren().clear();
        if (hotels.isEmpty()) {
            Label empty = new Label("No hotels found.");
            empty.getStyleClass().add("text-muted");
            hotelCardContainer.add(empty, 0, 0);
            GridPane.setColumnSpan(empty, 3);
            GridPane.setHalignment(empty, HPos.CENTER);
            return;
        }

        for (int i = 0; i < hotels.size(); i++) {
            VBox card = createHotelCard(hotels.get(i), i == 0);
            int col = i % 3;
            int row = i / 3;
            hotelCardContainer.add(card, col, row);
            GridPane.setHalignment(card, HPos.CENTER);
            GridPane.setFillWidth(card, true);
            GridPane.setHgrow(card, Priority.ALWAYS);
        }
    }

    private VBox createHotelCard(Hotel hotel, boolean featured) {
        ObservableNumberValue responsiveCardWidth = Bindings.max(
            210.0,
            hotelCardContainer.widthProperty().subtract(44).divide(3.0)
        );
        ObservableNumberValue responsiveImageHeight = Bindings.multiply(responsiveCardWidth, 0.60);

        VBox card = new VBox(0);
        card.getStyleClass().add("hotel-list-card");
        card.setFillWidth(true);
        card.prefWidthProperty().bind(responsiveCardWidth);
        card.minWidthProperty().bind(responsiveCardWidth);
        card.maxWidthProperty().bind(responsiveCardWidth);

        StackPane imageWrap = new StackPane();
        imageWrap.getStyleClass().add("hotel-card-image-wrap");
        imageWrap.prefWidthProperty().bind(responsiveCardWidth);
        imageWrap.minWidthProperty().bind(responsiveCardWidth);
        imageWrap.maxWidthProperty().bind(responsiveCardWidth);
        imageWrap.prefHeightProperty().bind(responsiveImageHeight);
        imageWrap.minHeightProperty().bind(responsiveImageHeight);
        imageWrap.maxHeightProperty().bind(responsiveImageHeight);
        Rectangle imageClip = new Rectangle();
        imageClip.setArcWidth(36);
        imageClip.setArcHeight(36);
        imageWrap.layoutBoundsProperty().addListener((obs, oldBounds, bounds) -> {
            imageClip.setWidth(bounds.getWidth());
            imageClip.setHeight(bounds.getHeight());
        });
        imageWrap.setClip(imageClip);
        ImageView cover = new ImageView(resolveCoverImage(hotel));
        cover.fitWidthProperty().bind(responsiveCardWidth);
        cover.fitHeightProperty().bind(responsiveImageHeight);
        cover.setPreserveRatio(false);
        cover.getStyleClass().add("hotel-card-image");
        attachHoverZoom(cover);

        Label featuredBadge = new Label("Featured");
        featuredBadge.getStyleClass().add("hotel-featured-badge");
        featuredBadge.setVisible(featured);
        featuredBadge.setManaged(featured);
        StackPane.setAlignment(featuredBadge, javafx.geometry.Pos.TOP_LEFT);

        Label favorite = new Label("♡");
        favorite.getStyleClass().add("hotel-favorite-badge");
        StackPane.setAlignment(favorite, javafx.geometry.Pos.TOP_RIGHT);

        imageWrap.getChildren().addAll(cover, featuredBadge, favorite);

        VBox body = new VBox(12);
        body.getStyleClass().add("hotel-card-body");

        Label title = new Label(nullToFallback(hotel.getName(), "Hotel"));
        title.getStyleClass().add("hotel-list-title");

        Label location = new Label("📍 " + joinNonBlank(hotel.getCityName(), hotel.getCountryName()));
        location.getStyleClass().add("hotel-list-location");

        List<Room> rooms = roomService.getRoomsByHotelId(hotel.getId());
        List<HotelReview> reviews = hotelReviewService.getReviewsForHotel(hotel.getId());
        double avg = hotelReviewService.getAverageStarsForHotel(hotel.getId());

        HBox ratingRow = new HBox(10);
        Label score = new Label("★ " + String.format("%.1f", avg));
        score.getStyleClass().add("hotel-rating-chip");
        Label reviewCount = new Label("(" + reviews.size() + " reviews)");
        reviewCount.getStyleClass().add("hotel-review-count");
        ratingRow.getChildren().addAll(score, reviewCount);

        HBox featureRow = new HBox(8);
        featureRow.getStyleClass().add("hotel-feature-row");
        for (String feature : buildFeatures(rooms)) {
            Label chip = new Label(feature);
            chip.getStyleClass().add("hotel-feature-chip");
            featureRow.getChildren().add(chip);
        }

        HBox priceRow = new HBox();
        priceRow.getStyleClass().add("hotel-price-row");
        Label price = new Label(formatMinPrice(rooms));
        price.getStyleClass().add("hotel-price-label");
        Button bookNow = new Button("Book Now");
        bookNow.getStyleClass().add("hotel-book-btn");
        bookNow.setOnAction(e -> openRoomSearchForHotel(hotel));
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        priceRow.getChildren().addAll(price, spacer, bookNow);

        body.getChildren().addAll(title, location, ratingRow, featureRow, priceRow);
        card.getChildren().addAll(imageWrap, body);
        return card;
    }

    private void attachHoverZoom(ImageView imageView) {
        ScaleTransition zoomIn = new ScaleTransition(Duration.millis(220), imageView);
        zoomIn.setToX(1.06);
        zoomIn.setToY(1.06);
        ScaleTransition zoomOut = new ScaleTransition(Duration.millis(220), imageView);
        zoomOut.setToX(1.0);
        zoomOut.setToY(1.0);
        imageView.setOnMouseEntered(e -> {
            zoomOut.stop();
            zoomIn.playFromStart();
        });
        imageView.setOnMouseExited(e -> {
            zoomIn.stop();
            zoomOut.playFromStart();
        });
    }

    private Image resolveCoverImage(Hotel hotel) {
        if (hotel != null && hotel.getImagePath() != null && !hotel.getImagePath().isBlank()) {
            Image img = ImageStorageService.loadFxImage(hotel.getImagePath());
            if (img != null) return img;
        }
        if (hotel != null) {
            List<Room> rooms = roomService.getRoomsByHotelId(hotel.getId());
            for (Room room : rooms) {
                if (room.getImagePath() != null && !room.getImagePath().isBlank()) {
                    Image img = ImageStorageService.loadFxImage(room.getImagePath());
                    if (img != null) return img;
                }
                var extra = roomImageService.getImagesForRoom(room.getId());
                for (var ri : extra) {
                    if (ri.getImagePath() == null || ri.getImagePath().isBlank()) continue;
                    Image img = ImageStorageService.loadFxImage(ri.getImagePath());
                    if (img != null) return img;
                }
            }
        }
        return null;
    }

    private List<String> buildFeatures(List<Room> rooms) {
        Set<String> features = new LinkedHashSet<>();
        features.add("WiFi");
        if (!rooms.isEmpty()) features.add("Restaurant");
        if (rooms.stream().anyMatch(r -> r.getCapacity() >= 3)) features.add("Family");
        if (rooms.stream().anyMatch(r -> r.getRoomType() == Room.RoomType.SUITE || r.getRoomType() == Room.RoomType.DELUXE)) {
            features.add("Pool");
        } else {
            features.add("Parking");
        }
        return new ArrayList<>(features).subList(0, Math.min(features.size(), 4));
    }

    private String formatMinPrice(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) return "See rooms";
        double min = rooms.stream().map(Room::getPricePerNight).min(Comparator.naturalOrder()).orElse(0.0);
        return String.format("$%.0f / night", min);
    }

    private void openRoomSearchForHotel(Hotel hotel) {
        if (hotel == null) return;
        RoomSearchController.setPreferredHotelId(hotel.getId());
        MainApp.navigateTo("room-search.fxml");
    }

    private String joinNonBlank(String first, String second) {
        String a = first == null ? "" : first.trim();
        String b = second == null ? "" : second.trim();
        if (a.isBlank()) return b;
        if (b.isBlank()) return a;
        return a + ", " + b;
    }

    private String nullToFallback(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value;
    }
}
