package com.hotel.model;

public class SelectedExtraService extends Service {
    private final String code;
    private final String billingType;
    private final int quantity;

    public SelectedExtraService(String code, String name, double price, String billingType, int quantity) {
        super(name, price);
        this.code = code;
        this.billingType = billingType == null ? "PER_NIGHT" : billingType;
        this.quantity = Math.max(1, quantity);
    }

    public String getCode() { return code; }
    public String getBillingType() { return billingType; }
    public int getQuantity() { return quantity; }
}

