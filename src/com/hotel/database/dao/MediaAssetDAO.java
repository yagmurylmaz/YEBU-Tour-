package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class MediaAssetDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public record MediaAsset(String pathKey, String mimeType, byte[] data) {}

    public void upsert(String pathKey, String mimeType, byte[] data) {
        String sql = """
            INSERT INTO media_assets (path_key, mime_type, data)
            VALUES (?,?,?)
            ON DUPLICATE KEY UPDATE
              mime_type = VALUES(mime_type),
              data = VALUES(data)
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pathKey);
            ps.setString(2, mimeType);
            ps.setBytes(3, data);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("upsert media asset failed: " + e.getMessage(), e);
        }
    }

    public Optional<MediaAsset> findByPathKey(String pathKey) {
        String sql = "SELECT path_key, mime_type, data FROM media_assets WHERE path_key = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pathKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new MediaAsset(
                    rs.getString("path_key"),
                    rs.getString("mime_type"),
                    rs.getBytes("data")
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("find media asset failed: " + e.getMessage(), e);
        }
    }

    public Optional<MediaAsset> findLatestByFilename(String filename) {
        String sql = """
            SELECT path_key, mime_type, data
            FROM media_assets
            WHERE path_key LIKE ?
            ORDER BY updated_at DESC
            LIMIT 1
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%/" + filename);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new MediaAsset(
                    rs.getString("path_key"),
                    rs.getString("mime_type"),
                    rs.getBytes("data")
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("find media asset by filename failed: " + e.getMessage(), e);
        }
    }

    public boolean deleteByPathKey(String pathKey) {
        String sql = "DELETE FROM media_assets WHERE path_key = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pathKey);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("delete media asset failed: " + e.getMessage(), e);
        }
    }
}
