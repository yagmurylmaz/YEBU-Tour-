package com.hotel.model;

public abstract class Service {
    private final String name;
    private final double price;

    protected Service(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
}
