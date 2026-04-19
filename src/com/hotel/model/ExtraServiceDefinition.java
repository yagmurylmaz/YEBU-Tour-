package com.hotel.model;

public class ExtraServiceDefinition {
    private int id;
    private String code;
    private String name;
    private String description;
    private double price;
    private String billingType;
    private boolean active;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description == null ? "" : description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getBillingType() { return billingType == null ? "PER_NIGHT" : billingType; }
    public void setBillingType(String billingType) { this.billingType = billingType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

