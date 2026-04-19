package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

public class UserDAO implements IUserDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, full_name, email, password_hash, phone, role FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapUser(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("findByEmail failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean emailExists(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public boolean updatePasswordHash(int userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            throw new IllegalStateException("updatePasswordHash failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean save(User user) {
        if (emailExists(user.getEmail())) return false;
        String sql = "INSERT INTO users (full_name, email, password_hash, phone, role) VALUES (?,?,?,?,?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getRole());
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getInt(1));
            }
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("save user failed: " + e.getMessage(), e);
        }
    }

    static User mapUser(ResultSet rs) throws Exception {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password_hash"));
        u.setPhone(rs.getString("phone"));
        u.setRole(rs.getString("role"));
        return u;
    }
}
