package com.hotel.model;

public class City {
    private int id;
    private int countryId;
    private String name;

    public City() {}

    public City(int countryId, String name) {
        this.countryId = countryId;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCountryId() { return countryId; }
    public void setCountryId(int countryId) { this.countryId = countryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}

