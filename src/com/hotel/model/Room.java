package com.hotel.model;

public class Room {
    public enum RoomType { SINGLE, DOUBLE, SUITE, DELUXE }

    private int id;
    private String roomNumber;
    private RoomType roomType;
    private double pricePerNight;
    private int capacity;
    private String description;
    /** Absolute path to a local image file; may be null if no photo. */
    private String imagePath;
    private Integer hotelId;
    private String hotelName;
    private String hotelImagePath;
    private boolean available = true;

    public Room() {}

    public Room(String roomNumber, RoomType roomType, double pricePerNight, int capacity, String description) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }
    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getDescription() { return description == null ? "" : description; }
    public void setDescription(String description) { this.description = description; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public Integer getHotelId() { return hotelId; }
    public void setHotelId(Integer hotelId) { this.hotelId = hotelId; }
    public String getHotelName() { return hotelName == null ? "" : hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public String getHotelImagePath() { return hotelImagePath; }
    public void setHotelImagePath(String hotelImagePath) { this.hotelImagePath = hotelImagePath; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getRoomTypeDisplay() {
        if (roomType == null) return "-";
        return switch (roomType) {
            case SINGLE -> "Single";
            case DOUBLE -> "Double";
            case SUITE -> "Suite";
            case DELUXE -> "Deluxe";
        };
    }

    public String getFormattedPrice() {
        return String.format("₺%.2f", pricePerNight);
    }
}
