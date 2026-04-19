package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.City;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CityDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<City> findByCountryId(int countryId) {
        String sql = "SELECT id, country_id, name FROM cities WHERE country_id=? ORDER BY name";
        List<City> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, countryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    City ci = new City();
                    ci.setId(rs.getInt("id"));
                    ci.setCountryId(rs.getInt("country_id"));
                    ci.setName(rs.getString("name"));
                    list.add(ci);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("find cities failed: " + e.getMessage(), e);
        }
        return list;
    }

    public int add(City city) {
        String sql = "INSERT INTO cities (country_id, name) VALUES (?,?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, city.getCountryId());
            ps.setString(2, city.getName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    city.setId(id);
                    return id;
                }
            }
            return -1;
        } catch (Exception e) {
            throw new IllegalStateException("add city failed: " + e.getMessage(), e);
        }
    }

    public Optional<City> findByCountryIdAndName(int countryId, String name) {
        String sql = """
            SELECT id, country_id, name FROM cities
            WHERE country_id=? AND LOWER(name)=LOWER(?)
            LIMIT 1
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, countryId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                City ci = new City();
                ci.setId(rs.getInt("id"));
                ci.setCountryId(rs.getInt("country_id"));
                ci.setName(rs.getString("name"));
                return Optional.of(ci);
            }
        } catch (Exception e) {
            throw new IllegalStateException("find city by country and name failed: " + e.getMessage(), e);
        }
    }
}

