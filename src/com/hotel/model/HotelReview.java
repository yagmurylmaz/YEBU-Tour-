package com.hotel.model;

public class HotelReview {
    private int id;
    private int reservationId;
    private int hotelId;
    private int customerId;
    private int stars;
    private String comment;
    private String createdAt;
    private String customerName;
    private String hotelName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public String getComment() { return comment == null ? "" : comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getCreatedAt() { return createdAt == null ? "" : createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getCustomerName() { return customerName == null ? "-" : customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getHotelName() { return hotelName == null ? "-" : hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
}
