package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.ExtraServiceDefinition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ExtraServiceDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<ExtraServiceDefinition> findActive() {
        String sql = """
            SELECT id, code, name, description, price, billing_type, active
            FROM extra_services
            WHERE active=1
            ORDER BY id
            """;
        List<ExtraServiceDefinition> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ExtraServiceDefinition d = new ExtraServiceDefinition();
                d.setId(rs.getInt("id"));
                d.setCode(rs.getString("code"));
                d.setName(rs.getString("name"));
                d.setDescription(rs.getString("description"));
                d.setPrice(rs.getDouble("price"));
                d.setBillingType(rs.getString("billing_type"));
                d.setActive(rs.getInt("active") == 1);
                list.add(d);
            }
        } catch (Exception e) {
            throw new IllegalStateException("find active extra services failed: " + e.getMessage(), e);
        }
        return list;
    }

    public List<ExtraServiceDefinition> findAll() {
        String sql = """
            SELECT id, code, name, description, price, billing_type, active
            FROM extra_services
            ORDER BY id
            """;
        List<ExtraServiceDefinition> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ExtraServiceDefinition d = new ExtraServiceDefinition();
                d.setId(rs.getInt("id"));
                d.setCode(rs.getString("code"));
                d.setName(rs.getString("name"));
                d.setDescription(rs.getString("description"));
                d.setPrice(rs.getDouble("price"));
                d.setBillingType(rs.getString("billing_type"));
                d.setActive(rs.getInt("active") == 1);
                list.add(d);
            }
        } catch (Exception e) {
            throw new IllegalStateException("find all extra services failed: " + e.getMessage(), e);
        }
        return list;
    }

    public int add(ExtraServiceDefinition d) {
        String sql = """
            INSERT INTO extra_services (code, name, description, price, billing_type, active)
            VALUES (?,?,?,?,?,?)
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getCode());
            ps.setString(2, d.getName());
            ps.setString(3, d.getDescription());
            ps.setDouble(4, d.getPrice());
            ps.setString(5, d.getBillingType());
            ps.setInt(6, d.isActive() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            return -1;
        } catch (Exception e) {
            throw new IllegalStateException("add extra service failed: " + e.getMessage(), e);
        }
    }

    public boolean update(ExtraServiceDefinition d) {
        String sql = """
            UPDATE extra_services
            SET code=?, name=?, description=?, price=?, billing_type=?, active=?
            WHERE id=?
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getCode());
            ps.setString(2, d.getName());
            ps.setString(3, d.getDescription());
            ps.setDouble(4, d.getPrice());
            ps.setString(5, d.getBillingType());
            ps.setInt(6, d.isActive() ? 1 : 0);
            ps.setInt(7, d.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("update extra service failed: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM extra_services WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("delete extra service failed: " + e.getMessage(), e);
        }
    }

    public int upsertDefaults(List<ExtraServiceDefinition> defaults) {
        String sql = """
            INSERT INTO extra_services (code, name, description, price, billing_type, active)
            VALUES (?,?,?,?,?,1)
            ON DUPLICATE KEY UPDATE
              name=VALUES(name),
              description=VALUES(description),
              price=VALUES(price),
              billing_type=VALUES(billing_type)
            """;
        int affected = 0;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (ExtraServiceDefinition d : defaults) {
                ps.setString(1, d.getCode());
                ps.setString(2, d.getName());
                ps.setString(3, d.getDescription());
                ps.setDouble(4, d.getPrice());
                ps.setString(5, d.getBillingType());
                affected += ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new IllegalStateException("upsert default extra services failed: " + e.getMessage(), e);
        }
        return affected;
    }
}

