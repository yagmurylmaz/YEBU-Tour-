package com.hotel.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class DatabaseConnection {

    private static DatabaseConnection instance;
    private final Properties props;
    private volatile boolean schemaReady;

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver not on classpath. Add mysql-connector-j.jar to lib/ (see README).", e);
        }
        this.props = DbConfig.load();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) instance = new DatabaseConnection();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(
            props.getProperty("db.url"),
            props.getProperty("db.user"),
            props.getProperty("db.password", "")
        );
        return c;
    }

    public synchronized void ensureSchema() {
        if (schemaReady) return;
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  full_name VARCHAR(255) NOT NULL,
                  email VARCHAR(255) NOT NULL UNIQUE,
                  password_hash VARCHAR(255) NOT NULL,
                  phone VARCHAR(64) NOT NULL,
                  role VARCHAR(32) NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS rooms (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  room_number VARCHAR(64) NOT NULL,
                  room_type VARCHAR(32) NOT NULL,
                  price_per_night DECIMAL(12,2) NOT NULL,
                  capacity INT NOT NULL,
                  description VARCHAR(1024) NOT NULL DEFAULT '',
                  available TINYINT(1) NOT NULL DEFAULT 1
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reservations (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  customer_id INT NOT NULL,
                  room_id INT NOT NULL,
                  check_in DATE NOT NULL,
                  check_out DATE NOT NULL,
                  total_price DECIMAL(14,2) NOT NULL,
                  status VARCHAR(32) NOT NULL,
                  created_at VARCHAR(32),
                  CONSTRAINT fk_res_user FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
                  CONSTRAINT fk_res_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS password_reset_tokens (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  user_id INT NOT NULL,
                  token_hash CHAR(64) NOT NULL,
                  expires_at DATETIME NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE KEY uq_prt_token (token_hash),
                  UNIQUE KEY uq_prt_user (user_id),
                  CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            schemaReady = true;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create database schema: " + e.getMessage(), e);
        }
    }

    public void closeConnection() {
        // Connections are short-lived; nothing to close globally.
    }
}
