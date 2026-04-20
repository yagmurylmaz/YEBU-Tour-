package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class RegistrationVerificationDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public record PendingRegistration(
        String email,
        String fullName,
        String phone,
        String passwordHash,
        String codeHash,
        Instant expiresAt
    ) {}

    public void replacePending(String email, String fullName, String phone, String passwordHash, String codeHash, Instant expiresAt) {
        String del = "DELETE FROM registration_verification_tokens WHERE email = ?";
        String ins = """
            INSERT INTO registration_verification_tokens (email, full_name, phone, password_hash, code_hash, expires_at)
            VALUES (?,?,?,?,?,?)
            """;
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement d = c.prepareStatement(del)) {
                d.setString(1, email);
                d.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement(ins)) {
                p.setString(1, email);
                p.setString(2, fullName);
                p.setString(3, phone);
                p.setString(4, passwordHash);
                p.setString(5, codeHash);
                p.setTimestamp(6, Timestamp.from(expiresAt));
                p.executeUpdate();
            }
            c.commit();
        } catch (Exception e) {
            throw new IllegalStateException("replacePending registration failed: " + e.getMessage(), e);
        }
    }

    public Optional<PendingRegistration> findValidByEmailAndCodeHash(String email, String codeHash) {
        String sql = """
            SELECT email, full_name, phone, password_hash, code_hash, expires_at
            FROM registration_verification_tokens
            WHERE email = ? AND code_hash = ? AND expires_at > ?
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, codeHash);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new PendingRegistration(
                    rs.getString("email"),
                    rs.getString("full_name"),
                    rs.getString("phone"),
                    rs.getString("password_hash"),
                    rs.getString("code_hash"),
                    rs.getTimestamp("expires_at").toInstant()
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("findValidByEmailAndCodeHash failed: " + e.getMessage(), e);
        }
    }

    public void deleteByEmail(String email) {
        String sql = "DELETE FROM registration_verification_tokens WHERE email = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("deleteByEmail failed: " + e.getMessage(), e);
        }
    }
}
