package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.Hotel;
import com.hotel.model.HotelReview;
import com.hotel.model.Room;
import com.hotel.model.User;
import com.hotel.service.HotelReviewService;
import com.hotel.service.HotelService;
import com.hotel.service.FavoriteHotelService;
import com.hotel.service.RoomImageService;
import com.hotel.service.RoomService;
import com.hotel.util.ImageStorageService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableNumberValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.geometry.HPos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class CustomerDashboardController {

    @FXML private BorderPane rootPane;
    @FXML private Label welcomeLabel;
    @FXML private Label supportStatusLabel;
    @FXML private GridPane hotelCardContainer;
    @FXML private Button favoriteFilterButton;
    @FXML private ScrollPane hotelScrollPane;
    @FXML private Button loadMoreButton;
    @FXML private ToggleButton darkModeButton;

    private final HotelService hotelService = new HotelService();
    private final HotelReviewService hotelReviewService = new HotelReviewService();
    private final FavoriteHotelService favoriteHotelService = new FavoriteHotelService();
    private final RoomService roomService = new RoomService();
    private final RoomImageService roomImageService = new RoomImageService();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "customer-dashboard-bg");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong renderToken = new AtomicLong(0);
    private static final int PAGE_SIZE = 9;
    private List<Hotel> allHotels = List.of();
    private Set<Integer> favoriteHotelIds = new LinkedHashSet<>();
    private boolean showOnlyFavorites = false;
    private int visibleHotelCount = PAGE_SIZE;
    private boolean autoLoadingMore = false;
    private String activeFilterKey = "";
    private int renderedHotelCount = 0;
    private boolean pendingAppend = false;
    private boolean hasCompletedInitialRender = false;
    private boolean isNearBottomForLoadMore = false;
    private boolean lastRenderFailed = false;
    private boolean darkModeEnabled = false;
    private final Map<Integer, HotelCardData> hotelCardDataCache = new java.util.concurrent.ConcurrentHashMap<>();

    @FXML
    private void initialize() {
        User user = SessionManager.getInstance().getLoggedInUser();
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getFullName() + "!");
        }
        allHotels = hotelService.getAllHotels();
        if (user != null) {
            favoriteHotelIds = new LinkedHashSet<>(favoriteHotelService.getFavoriteHotelIdsForUser(user.getId()));
        }
        darkModeEnabled = SessionManager.getInstance().isDarkModeEnabled();
        if (darkModeButton != null) {
            Region thumb = new Region();
            thumb.getStyleClass().add("dark-mode-switch-thumb");
            darkModeButton.setGraphic(thumb);
            darkModeButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            darkModeButton.setSelected(darkModeEnabled);
        }
        applyTheme();
        hotelCardDataCache.clear();
        setupHotelGridColumns();
        setupInfiniteScroll();
        refreshFavoriteFilterButton();
        refreshLoadMoreButton();
        Platform.runLater(this::updateLoadMoreVisibilityFromScroll);
        renderHotelCardsAsync();
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

    private void renderHotelCardsAsync() {
        if (hotelCardContainer == null) return;
        long token = renderToken.incrementAndGet();
        List<Hotel> filteredHotels = getFilteredHotels();
        String filterKey = buildFilterKey(filteredHotels);
        boolean append = pendingAppend && filterKey.equals(activeFilterKey);
        pendingAppend = false;
        if (!append) {
            activeFilterKey = filterKey;
            renderedHotelCount = 0;
            if (!hasCompletedInitialRender || lastRenderFailed) {
                hotelCardContainer.getChildren().clear();
                showSkeletonCards();
            }
        }
        backgroundExecutor.submit(() -> {
            try {
                List<HotelCardData> filteredCardData = prepareCardData(filteredHotels);
                List<HotelCardData> visibleCardData = limitVisibleCardData(filteredCardData);
                int featuredHotelId = pickFeaturedHotelId(filteredCardData);
                Platform.runLater(() -> renderPreparedCards(token, visibleCardData, featuredHotelId, append, filterKey));
            } catch (Exception ex) {
                Platform.runLater(() -> renderLoadingError(token, ex));
            }
        });
    }

    private void renderLoadingError(long token, Exception ex) {
        if (hotelCardContainer == null) return;
        if (token != renderToken.get()) return;
        hotelCardContainer.getChildren().clear();
        Label error = new Label("Hotels could not be loaded. Please try again.");
        error.getStyleClass().add("error-label");
        hotelCardContainer.add(error, 0, 0);
        GridPane.setColumnSpan(error, 3);
        GridPane.setHalignment(error, HPos.CENTER);
        autoLoadingMore = false;
        pendingAppend = false;
        lastRenderFailed = true;
        refreshLoadMoreButton();
        System.err.println("[CustomerDashboard] Hotel render failed: " + ex.getMessage());
    }

    private void renderPreparedCards(long token, List<HotelCardData> cardDataList, int featuredHotelId, boolean append, String filterKey) {
        if (hotelCardContainer == null) return;
        if (token != renderToken.get()) return;
        if (!append || !filterKey.equals(activeFilterKey)) {
            hotelCardContainer.getChildren().clear();
            renderedHotelCount = 0;
            activeFilterKey = filterKey;
        }
        if (cardDataList.isEmpty()) {
            Label empty = new Label("No hotels found.");
            empty.getStyleClass().add("text-muted");
            hotelCardContainer.add(empty, 0, 0);
            GridPane.setColumnSpan(empty, 3);
            GridPane.setHalignment(empty, HPos.CENTER);
            autoLoadingMore = false;
            hasCompletedInitialRender = true;
            lastRenderFailed = false;
            refreshLoadMoreButton();
            return;
        }

        int startIndex = append ? renderedHotelCount : 0;
        startIndex = Math.min(startIndex, cardDataList.size());
        for (int i = startIndex; i < cardDataList.size(); i++) {
            HotelCardData data = cardDataList.get(i);
            VBox card = createHotelCard(data, data.hotel().getId() == featuredHotelId);
            int col = i % 3;
            int row = i / 3;
            hotelCardContainer.add(card, col, row);
            GridPane.setHalignment(card, HPos.CENTER);
            GridPane.setFillWidth(card, true);
            GridPane.setHgrow(card, Priority.ALWAYS);
            playCardFadeIn(card, i - startIndex);
        }
        renderedHotelCount = cardDataList.size();
        autoLoadingMore = false;
        hasCompletedInitialRender = true;
        lastRenderFailed = false;
        refreshLoadMoreButton();
    }

    private VBox createHotelCard(HotelCardData data, boolean featured) {
        Hotel hotel = data.hotel();
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
        ImageView cover = new ImageView(data.coverImage());
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

        Button favorite = new Button();
        favorite.setText("");
        favorite.setFocusTraversable(false);
        favorite.getStyleClass().add("hotel-favorite-badge");
        updateFavoriteIcon(favorite, isFavoriteHotel(hotel.getId()));
        favorite.setOnAction(e -> handleFavoriteToggle(hotel, favorite));
        StackPane.setAlignment(favorite, javafx.geometry.Pos.TOP_RIGHT);

        imageWrap.getChildren().addAll(cover, featuredBadge, favorite);

        VBox body = new VBox(12);
        body.getStyleClass().add("hotel-card-body");

        Label title = new Label(nullToFallback(hotel.getName(), "Hotel"));
        title.getStyleClass().add("hotel-list-title");

        Label location = new Label("📍 " + joinNonBlank(hotel.getCityName(), hotel.getCountryName()));
        location.getStyleClass().add("hotel-list-location");

        List<Room> rooms = data.rooms();
        List<HotelReview> reviews = data.reviews();
        double avg = data.avgStars();

        HBox ratingRow = new HBox(10);
        Label score = new Label("★ " + String.format("%.1f", avg));
        score.getStyleClass().add("hotel-rating-chip");
        Label reviewCount = new Label("(" + reviews.size() + " reviews)");
        reviewCount.getStyleClass().add("hotel-review-count");
        ratingRow.getChildren().addAll(score, reviewCount);

        HBox featureRow = new HBox(8);
        featureRow.getStyleClass().add("hotel-feature-row");
        for (String feature : data.features()) {
            Label chip = new Label(feature);
            chip.getStyleClass().add("hotel-feature-chip");
            featureRow.getChildren().add(chip);
        }

        HBox priceRow = new HBox();
        priceRow.getStyleClass().add("hotel-price-row");
        Label price = new Label(data.minPriceText());
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

    private Image resolveCoverImage(Hotel hotel, List<Room> rooms) {
        if (hotel != null && hotel.getImagePath() != null && !hotel.getImagePath().isBlank()) {
            Image img = ImageStorageService.loadFxImage(hotel.getImagePath());
            if (img != null) return img;
        }
        if (hotel != null && rooms != null) {
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
        return String.format("₺%.0f / night", min);
    }

    private void openRoomSearchForHotel(Hotel hotel) {
        if (hotel == null) return;
        RoomSearchController.setPreferredHotelId(hotel.getId());
        MainApp.navigateTo("room-search.fxml");
    }

    @FXML
    private void handleToggleFavoritesFilter() {
        showOnlyFavorites = !showOnlyFavorites;
        visibleHotelCount = PAGE_SIZE;
        refreshFavoriteFilterButton();
        refreshLoadMoreButton();
        renderHotelCardsAsync();
    }

    @FXML
    private void handleToggleDarkMode() {
        if (darkModeButton == null) return;
        darkModeEnabled = darkModeButton.isSelected();
        SessionManager.getInstance().setDarkModeEnabled(darkModeEnabled);
        applyTheme();
    }

    @FXML
    private void handleLoadMoreHotels() {
        int total = getFilteredHotels().size();
        if (visibleHotelCount >= total) {
            refreshLoadMoreButton();
            return;
        }
        autoLoadingMore = true;
        pendingAppend = true;
        visibleHotelCount += PAGE_SIZE;
        refreshLoadMoreButton();
        renderHotelCardsAsync();
    }

    private void setupInfiniteScroll() {
        if (hotelScrollPane == null) return;
        hotelScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> updateLoadMoreVisibilityFromScroll());
        hotelScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> updateLoadMoreVisibilityFromScroll());
        if (hotelScrollPane.getContent() != null) {
            hotelScrollPane.getContent().layoutBoundsProperty().addListener((obs, oldVal, newVal) -> updateLoadMoreVisibilityFromScroll());
        }
    }

    private void handleFavoriteToggle(Hotel hotel, Button sourceButton) {
        if (hotel == null) return;
        User user = SessionManager.getInstance().getLoggedInUser();
        if (user == null) return;
        playFavoritePopAnimation(sourceButton, () -> {
            boolean wasFavorite = favoriteHotelIds.contains(hotel.getId());
            boolean expectedFavorite = !wasFavorite;
            if (expectedFavorite) {
                favoriteHotelIds.add(hotel.getId());
            } else {
                favoriteHotelIds.remove(hotel.getId());
            }
            updateFavoriteIcon(sourceButton, expectedFavorite);
            sourceButton.setDisable(true);
            sourceButton.setOpacity(0.8);
            backgroundExecutor.submit(() -> {
                boolean nowFavorite;
                Exception failure = null;
                try {
                    nowFavorite = favoriteHotelService.toggleFavorite(user.getId(), hotel.getId());
                } catch (Exception ex) {
                    nowFavorite = wasFavorite;
                    failure = ex;
                }
                boolean finalNowFavorite = nowFavorite;
                Exception finalFailure = failure;
                Platform.runLater(() -> {
                    if (finalFailure != null) {
                        if (wasFavorite) {
                            favoriteHotelIds.add(hotel.getId());
                        } else {
                            favoriteHotelIds.remove(hotel.getId());
                        }
                        updateFavoriteIcon(sourceButton, wasFavorite);
                        showNonBlockingWarning("Favorite update failed. Please try again.");
                    } else if (finalNowFavorite != expectedFavorite) {
                        if (finalNowFavorite) {
                            favoriteHotelIds.add(hotel.getId());
                        } else {
                            favoriteHotelIds.remove(hotel.getId());
                        }
                        updateFavoriteIcon(sourceButton, finalNowFavorite);
                    }
                    refreshFavoriteFilterButton();
                    if (showOnlyFavorites && !favoriteHotelIds.contains(hotel.getId())) {
                        renderHotelCardsAsync();
                    }
                    sourceButton.setDisable(false);
                    sourceButton.setOpacity(1.0);
                });
            });
        });
    }

    private void updateFavoriteIcon(Button favoriteButton, boolean isFavorite) {
        SVGPath heart = new SVGPath();
        heart.setContent("M12 21s-6.7-4.35-9.33-8.24C0.54 9.62 1.6 5.52 4.86 4.2c2.2-.89 4.2-.08 5.39 1.41 1.19-1.49 3.19-2.3 5.39-1.41 3.26 1.32 4.32 5.42 2.19 8.56C18.7 16.65 12 21 12 21z");
        heart.setScaleX(0.82);
        heart.setScaleY(0.82);
        heart.setFill(isFavorite ? Color.web("#dc2626") : Color.TRANSPARENT);
        heart.setStroke(isFavorite ? Color.web("#dc2626") : Color.web("#f8fafc"));
        heart.setStrokeWidth(1.8);
        favoriteButton.setGraphic(heart);
    }

    private void playFavoritePopAnimation(Button button, Runnable onFinished) {
        ScaleTransition popIn = new ScaleTransition(Duration.millis(95), button);
        popIn.setToX(1.18);
        popIn.setToY(1.18);
        ScaleTransition popOut = new ScaleTransition(Duration.millis(120), button);
        popOut.setToX(1.0);
        popOut.setToY(1.0);
        popIn.setOnFinished(e -> popOut.playFromStart());
        popOut.setOnFinished(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
            if (onFinished != null) onFinished.run();
        });
        popIn.playFromStart();
    }

    private void playCardFadeIn(VBox card, int sequenceIndex) {
        if (card == null) return;
        card.setOpacity(0.0);
        FadeTransition fade = new FadeTransition(Duration.millis(170), card);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setDelay(Duration.millis(Math.min(sequenceIndex * 20L, 100)));
        fade.play();
    }

    private void showSkeletonCards() {
        if (hotelCardContainer == null) return;
        for (int i = 0; i < 6; i++) {
            VBox skeleton = createSkeletonCard();
            int col = i % 3;
            int row = i / 3;
            hotelCardContainer.add(skeleton, col, row);
            GridPane.setHalignment(skeleton, HPos.CENTER);
            GridPane.setFillWidth(skeleton, true);
            GridPane.setHgrow(skeleton, Priority.ALWAYS);
        }
    }

    private VBox createSkeletonCard() {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("hotel-list-card", "skeleton-card");
        card.setPrefWidth(260);
        card.setMaxWidth(Double.MAX_VALUE);

        Region image = new Region();
        image.getStyleClass().addAll("skeleton-block", "skeleton-image");

        VBox body = new VBox(10);
        body.getStyleClass().add("hotel-card-body");

        Region line1 = new Region();
        line1.getStyleClass().addAll("skeleton-block", "skeleton-line-title");
        Region line2 = new Region();
        line2.getStyleClass().addAll("skeleton-block", "skeleton-line-short");
        Region line3 = new Region();
        line3.getStyleClass().addAll("skeleton-block", "skeleton-line-medium");

        body.getChildren().addAll(line1, line2, line3);
        card.getChildren().addAll(image, body);
        return card;
    }

    private void showNonBlockingWarning(String message) {
        if (favoriteFilterButton == null || message == null || message.isBlank()) return;
        Tooltip tip = new Tooltip(message);
        tip.setAutoHide(true);
        tip.setShowDelay(Duration.ZERO);
        tip.setHideDelay(Duration.seconds(2.5));
        favoriteFilterButton.setTooltip(tip);
        var screenPoint = favoriteFilterButton.localToScreen(0, favoriteFilterButton.getHeight());
        if (screenPoint == null) return;
        tip.show(favoriteFilterButton, screenPoint.getX(), screenPoint.getY() + 6);
        PauseTransition cleanup = new PauseTransition(Duration.seconds(2.6));
        cleanup.setOnFinished(e -> {
            if (favoriteFilterButton.getTooltip() == tip) {
                favoriteFilterButton.setTooltip(null);
            }
        });
        cleanup.play();
    }

    private List<HotelCardData> prepareCardData(List<Hotel> hotels) {
        if (hotels == null || hotels.isEmpty()) return List.of();
        List<HotelCardData> out = new ArrayList<>();
        for (Hotel hotel : hotels) {
            HotelCardData cached = hotelCardDataCache.get(hotel.getId());
            if (cached == null || cached.hotel() == null || !sameHotelSnapshot(cached.hotel(), hotel)) {
                cached = buildHotelCardData(hotel);
                hotelCardDataCache.put(hotel.getId(), cached);
            }
            out.add(cached);
        }
        return out;
    }

    private HotelCardData buildHotelCardData(Hotel hotel) {
        List<Room> rooms = roomService.getRoomsByHotelId(hotel.getId());
        List<HotelReview> reviews = hotelReviewService.getReviewsForHotel(hotel.getId());
        double avg = hotelReviewService.getAverageStarsForHotel(hotel.getId());
        Image cover = resolveCoverImage(hotel, rooms);
        List<String> features = buildFeatures(rooms);
        String minPriceText = formatMinPrice(rooms);
        return new HotelCardData(hotel, rooms, reviews, avg, cover, features, minPriceText);
    }

    private int pickFeaturedHotelId(List<HotelCardData> cardDataList) {
        if (cardDataList == null || cardDataList.isEmpty()) return -1;
        int featuredId = cardDataList.get(0).hotel().getId();
        double maxAvg = -1.0;
        for (HotelCardData data : cardDataList) {
            if (data.avgStars() > maxAvg) {
                maxAvg = data.avgStars();
                featuredId = data.hotel().getId();
            }
        }
        return featuredId;
    }

    private boolean sameHotelSnapshot(Hotel cached, Hotel latest) {
        if (cached == null || latest == null) return false;
        return cached.getId() == latest.getId()
            && java.util.Objects.equals(cached.getName(), latest.getName())
            && java.util.Objects.equals(cached.getCityName(), latest.getCityName())
            && java.util.Objects.equals(cached.getCountryName(), latest.getCountryName())
            && java.util.Objects.equals(cached.getImagePath(), latest.getImagePath());
    }

    private List<Hotel> getFilteredHotels() {
        if (!showOnlyFavorites) return allHotels;
        return favoriteHotelService.filterFavoriteHotels(allHotels, favoriteHotelIds);
    }

    private List<Hotel> limitVisibleHotels(List<Hotel> hotels) {
        if (hotels == null || hotels.isEmpty()) return List.of();
        int end = Math.min(visibleHotelCount, hotels.size());
        return hotels.subList(0, end);
    }

    private List<HotelCardData> limitVisibleCardData(List<HotelCardData> cardData) {
        if (cardData == null || cardData.isEmpty()) return List.of();
        int end = Math.min(visibleHotelCount, cardData.size());
        return cardData.subList(0, end);
    }

    private boolean isFavoriteHotel(int hotelId) {
        return favoriteHotelIds.contains(hotelId);
    }

    private void refreshFavoriteFilterButton() {
        if (favoriteFilterButton == null) return;
        favoriteFilterButton.getStyleClass().remove("btn-primary");
        if (showOnlyFavorites) {
            favoriteFilterButton.setText("All Hotels");
            favoriteFilterButton.getStyleClass().add("btn-primary");
        } else {
            favoriteFilterButton.setText("My Favorites");
        }
    }

    private void applyTheme() {
        if (rootPane == null) return;
        if (darkModeEnabled) {
            if (!rootPane.getStyleClass().contains("dark-mode")) {
                rootPane.getStyleClass().add("dark-mode");
            }
        } else {
            rootPane.getStyleClass().remove("dark-mode");
        }
        if (darkModeButton != null) {
            darkModeButton.setSelected(darkModeEnabled);
        }
    }

    private void refreshLoadMoreButton() {
        if (loadMoreButton == null) return;
        int total = getFilteredHotels().size();
        boolean hasMore = visibleHotelCount < total;
        boolean shouldShow = hasMore && isNearBottomForLoadMore;
        loadMoreButton.setVisible(shouldShow);
        loadMoreButton.setManaged(shouldShow);
        loadMoreButton.setDisable(autoLoadingMore);
        if (hasMore) {
            int remaining = Math.max(total - visibleHotelCount, 0);
            loadMoreButton.setText("Load More (" + remaining + " left)");
        }
    }

    private void updateLoadMoreVisibilityFromScroll() {
        if (hotelScrollPane == null) return;
        isNearBottomForLoadMore = hotelScrollPane.getVvalue() >= 0.97;
        refreshLoadMoreButton();
    }

    private String buildFilterKey(List<Hotel> hotels) {
        if (hotels == null || hotels.isEmpty()) return showOnlyFavorites ? "fav:empty" : "all:empty";
        StringBuilder sb = new StringBuilder(showOnlyFavorites ? "fav:" : "all:");
        for (Hotel h : hotels) {
            sb.append(h.getId()).append(',');
        }
        return sb.toString();
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

    private record HotelCardData(
        Hotel hotel,
        List<Room> rooms,
        List<HotelReview> reviews,
        double avgStars,
        Image coverImage,
        List<String> features,
        String minPriceText
    ) {}
}
