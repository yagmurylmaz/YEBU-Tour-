package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.Country;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CountryDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<Country> findAll() {
        String sql = "SELECT id, name FROM countries ORDER BY name";
        List<Country> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Country co = new Country();
                co.setId(rs.getInt("id"));
                co.setName(rs.getString("name"));
                list.add(co);
            }
        } catch (Exception e) {
            throw new IllegalStateException("findAll countries failed: " + e.getMessage(), e);
        }
        return list;
    }

    public int add(Country country) {
        String sql = "INSERT INTO countries (name) VALUES (?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, country.getName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    country.setId(id);
                    return id;
                }
            }
            return -1;
        } catch (Exception e) {
            throw new IllegalStateException("add country failed: " + e.getMessage(), e);
        }
    }

    public Optional<Country> findByName(String name) {
        String sql = "SELECT id, name FROM countries WHERE LOWER(name)=LOWER(?) LIMIT 1";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Country co = new Country();
                co.setId(rs.getInt("id"));
                co.setName(rs.getString("name"));
                return Optional.of(co);
            }
        } catch (Exception e) {
            throw new IllegalStateException("find country by name failed: " + e.getMessage(), e);
        }
    }
}

