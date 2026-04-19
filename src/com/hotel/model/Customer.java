package com.hotel.model;

public class Customer extends User {
    public Customer(String fullName, String email, String password, String phone) {
        super(fullName, email, password, phone, "CUSTOMER");
    }
}
