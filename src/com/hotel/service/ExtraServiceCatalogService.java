package com.hotel.service;

import com.hotel.database.dao.ExtraServiceDAO;
import com.hotel.model.ExtraServiceDefinition;

import java.util.List;

public class ExtraServiceCatalogService {
    private final ExtraServiceDAO dao = new ExtraServiceDAO();

    public List<ExtraServiceDefinition> getActiveServices() {
        return dao.findActive();
    }

    public List<ExtraServiceDefinition> getAllServices() {
        return dao.findAll();
    }

    public int addService(ExtraServiceDefinition d) {
        validate(d);
        return dao.add(d);
    }

    public boolean updateService(ExtraServiceDefinition d) {
        if (d.getId() <= 0) throw new IllegalArgumentException("Select a service to update.");
        validate(d);
        return dao.update(d);
    }

    public boolean deleteService(int id) {
        return dao.delete(id);
    }

    public void ensureDefaultServices() {
        dao.upsertDefaults(List.of(
            build("BREAKFAST", "Breakfast", "Open buffet breakfast", 120.0, "PER_NIGHT"),
            build("GYM", "Gym", "Unlimited gym access", 90.0, "PER_NIGHT"),
            build("POOL", "Pool", "Indoor pool access", 75.0, "PER_NIGHT"),
            build("PARKING", "Parking", "Reserved parking spot", 60.0, "PER_NIGHT")
        ));
    }

    private static ExtraServiceDefinition build(String code, String name, String desc, double price, String billingType) {
        ExtraServiceDefinition d = new ExtraServiceDefinition();
        d.setCode(code);
        d.setName(name);
        d.setDescription(desc);
        d.setPrice(price);
        d.setBillingType(billingType);
        d.setActive(true);
        return d;
    }

    private static void validate(ExtraServiceDefinition d) {
        if (d.getCode() == null || d.getCode().isBlank()) throw new IllegalArgumentException("Code cannot be empty.");
        if (d.getName() == null || d.getName().isBlank()) throw new IllegalArgumentException("Name cannot be empty.");
        if (d.getPrice() < 0) throw new IllegalArgumentException("Price cannot be negative.");
        if (!"PER_NIGHT".equalsIgnoreCase(d.getBillingType()) && !"PER_STAY".equalsIgnoreCase(d.getBillingType())) {
            throw new IllegalArgumentException("Billing type must be PER_NIGHT or PER_STAY.");
        }
    }
}

