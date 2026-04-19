package com.hotel.model;

public class Room {
    public enum RoomType { SINGLE, DOUBLE, SUITE, DELUXE }

    private int id;
    private String roomNumber;
    private RoomType roomType;
    private double pricePerNight;
    private int capacity;
    private String description;
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
