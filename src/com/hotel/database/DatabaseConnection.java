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
                CREATE TABLE IF NOT EXISTS countries (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  name VARCHAR(128) NOT NULL UNIQUE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS cities (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  country_id INT NOT NULL,
                  name VARCHAR(128) NOT NULL,
                  UNIQUE KEY uq_city_country (country_id, name),
                  CONSTRAINT fk_city_country FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                  INDEX idx_city_country (country_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hotels (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  name VARCHAR(255) NOT NULL,
                  country_id INT NOT NULL,
                  city_id INT NOT NULL,
                  address_line VARCHAR(512) NOT NULL,
                  phone VARCHAR(64) NULL,
                  email VARCHAR(255) NULL,
                  image_path VARCHAR(1024) NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  CONSTRAINT fk_hotel_country FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE RESTRICT,
                  CONSTRAINT fk_hotel_city FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE RESTRICT,
                  INDEX idx_hotel_country (country_id),
                  INDEX idx_hotel_city (city_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            ensureHotelImageColumn(st);
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
                  available TINYINT(1) NOT NULL DEFAULT 1,
                  image_path VARCHAR(1024) NULL,
                  hotel_id INT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            ensureRoomImageColumn(st);
            ensureRoomHotelColumn(st);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS room_images (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  room_id INT NOT NULL,
                  image_path VARCHAR(1024) NOT NULL,
                  sort_order INT NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  CONSTRAINT fk_ri_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
                  INDEX idx_ri_room (room_id),
                  INDEX idx_ri_room_sort (room_id, sort_order)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS room_availability (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  room_id INT NOT NULL,
                  available_date DATE NOT NULL,
                  CONSTRAINT fk_ra_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
                  UNIQUE KEY uq_ra_room_date (room_id, available_date),
                  INDEX idx_ra_room_date (room_id, available_date)
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
                CREATE TABLE IF NOT EXISTS extra_services (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  code VARCHAR(64) NOT NULL UNIQUE,
                  name VARCHAR(128) NOT NULL,
                  description VARCHAR(512) NOT NULL DEFAULT '',
                  price DECIMAL(12,2) NOT NULL,
                  billing_type VARCHAR(32) NOT NULL DEFAULT 'PER_NIGHT',
                  active TINYINT(1) NOT NULL DEFAULT 1
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reservation_services (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  reservation_id INT NOT NULL,
                  service_code VARCHAR(64) NOT NULL,
                  service_name VARCHAR(128) NOT NULL,
                  unit_price DECIMAL(12,2) NOT NULL,
                  quantity INT NOT NULL DEFAULT 1,
                  billing_type VARCHAR(32) NOT NULL DEFAULT 'PER_NIGHT',
                  line_total DECIMAL(14,2) NOT NULL,
                  CONSTRAINT fk_rs_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
                  INDEX idx_rs_reservation (reservation_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hotel_reviews (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  reservation_id INT NOT NULL,
                  hotel_id INT NOT NULL,
                  customer_id INT NOT NULL,
                  stars INT NOT NULL,
                  comment VARCHAR(1500) NOT NULL,
                  created_at VARCHAR(32),
                  CONSTRAINT fk_hr_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
                  CONSTRAINT fk_hr_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE,
                  CONSTRAINT fk_hr_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
                  UNIQUE KEY uq_hr_reservation (reservation_id),
                  INDEX idx_hr_hotel (hotel_id),
                  INDEX idx_hr_customer (customer_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS app_meta (
                  meta_key VARCHAR(128) PRIMARY KEY,
                  meta_value VARCHAR(512) NULL,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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

    private static void ensureRoomImageColumn(java.sql.Statement st) throws SQLException {
        try {
            st.executeUpdate("ALTER TABLE rooms ADD COLUMN image_path VARCHAR(1024) NULL");
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (e.getErrorCode() == 1060 || msg.contains("Duplicate column")) {
                return;
            }
            throw e;
        }
    }

    private static void ensureRoomHotelColumn(java.sql.Statement st) throws SQLException {
        try {
            st.executeUpdate("ALTER TABLE rooms ADD COLUMN hotel_id INT NULL");
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (e.getErrorCode() == 1060 || msg.contains("Duplicate column")) {
                // already exists, continue for FK migration
            } else {
                throw e;
            }
        }
        try {
            st.executeUpdate("ALTER TABLE rooms DROP FOREIGN KEY fk_room_hotel");
        } catch (SQLException e) {
            // ignore if FK does not exist
        }
        try {
            st.executeUpdate("ALTER TABLE rooms ADD CONSTRAINT fk_room_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE");
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Duplicate") || msg.contains("already exists")) {
                return;
            }
            throw e;
        }
    }

    private static void ensureHotelImageColumn(java.sql.Statement st) throws SQLException {
        try {
            st.executeUpdate("ALTER TABLE hotels ADD COLUMN image_path VARCHAR(1024) NULL");
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (e.getErrorCode() == 1060 || msg.contains("Duplicate column")) {
                return;
            }
            throw e;
        }
    }

    public void closeConnection() {
        // Connections are short-lived; nothing to close globally.
    }
}
