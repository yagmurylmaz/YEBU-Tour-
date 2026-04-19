package com.hotel.model;

public class RoomImage {
    private int id;
    private int roomId;
    private String imagePath;
    private int sortOrder;

    public RoomImage() {}

    public RoomImage(int roomId, String imagePath, int sortOrder) {
        this.roomId = roomId;
        this.imagePath = imagePath;
        this.sortOrder = sortOrder;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}

