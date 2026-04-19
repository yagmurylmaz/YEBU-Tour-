package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class PasswordResetDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public void replaceToken(int userId, String tokenHash, Instant expiresAt) {
        String del = "DELETE FROM password_reset_tokens WHERE user_id = ?";
        String ins = """
            INSERT INTO password_reset_tokens (user_id, token_hash, expires_at)
            VALUES (?,?,?)
            """;
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement d = c.prepareStatement(del)) {
                d.setInt(1, userId);
                d.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement(ins)) {
                p.setInt(1, userId);
                p.setString(2, tokenHash);
                p.setTimestamp(3, Timestamp.from(expiresAt));
                p.executeUpdate();
            }
            c.commit();
        } catch (Exception e) {
            throw new IllegalStateException("replaceToken failed: " + e.getMessage(), e);
        }
    }

    /**
     * Avoids MySQL {@code NOW()} vs JVM/JDBC timezone skew: compare {@code expires_at} to the same kind of
     * {@link Timestamp} we used when inserting ({@link Timestamp#from(Instant)}).
     */
    public Optional<Integer> findValidUserIdByTokenHash(String tokenHash) {
        String sql = """
            SELECT user_id FROM password_reset_tokens
            WHERE token_hash = ? AND expires_at > ?
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(rs.getInt("user_id"));
            }
        } catch (Exception e) {
            throw new IllegalStateException("findValidUserIdByTokenHash failed: " + e.getMessage(), e);
        }
    }

    public void deleteByUserId(int userId) {
        String sql = "DELETE FROM password_reset_tokens WHERE user_id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("deleteByUserId failed: " + e.getMessage(), e);
        }
    }
}
