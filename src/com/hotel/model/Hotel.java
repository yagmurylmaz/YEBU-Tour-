package com.hotel.model;

public class Hotel {
    private int id;
    private String name;
    private int countryId;
    private int cityId;
    private String addressLine;
    private String phone;
    private String email;
    private String imagePath;

    // display-only (joined)
    private String countryName;
    private String cityName;

    public Hotel() {}

    public Hotel(String name, int countryId, int cityId, String addressLine, String phone, String email) {
        this.name = name;
        this.countryId = countryId;
        this.cityId = cityId;
        this.addressLine = addressLine;
        this.phone = phone;
        this.email = email;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCountryId() { return countryId; }
    public void setCountryId(int countryId) { this.countryId = countryId; }
    public int getCityId() { return cityId; }
    public void setCityId(int cityId) { this.cityId = cityId; }
    public String getAddressLine() { return addressLine == null ? "" : addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public String getPhone() { return phone == null ? "" : phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email == null ? "" : email; }
    public void setEmail(String email) { this.email = email; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getCountryName() { return countryName == null ? "" : countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public String getCityName() { return cityName == null ? "" : cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    @Override
    public String toString() {
        return getName();
    }
}

