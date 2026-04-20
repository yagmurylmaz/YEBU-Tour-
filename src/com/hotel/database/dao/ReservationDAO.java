package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.Reservation;
import com.hotel.model.Reservation.Status;
import com.hotel.model.Room;
import com.hotel.model.SelectedExtraService;
import com.hotel.model.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO implements IReservationDAO {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public int save(Reservation reservation, List<Service> services) {
        if (!isRoomAvailableForReservation(reservation)) return -1;
        reservation.setStatus(Status.PENDING);
        reservation.setCreatedAt(LocalDateTime.now().format(TS_FORMAT));
        String sql = """
            INSERT INTO reservations (customer_id, room_id, check_in, check_out, total_price, status, created_at)
            VALUES (?,?,?,?,?,?,?)
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            boolean oldAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                ps.setInt(1, reservation.getCustomerId());
                ps.setInt(2, reservation.getRoomId());
                ps.setObject(3, reservation.getCheckInDate());
                ps.setObject(4, reservation.getCheckOutDate());
                ps.setDouble(5, reservation.getTotalPrice());
                ps.setString(6, Status.PENDING.name());
                ps.setString(7, reservation.getCreatedAt());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        reservation.setId(id);
                        saveReservationServices(c, id, reservation, services);
                        c.commit();
                        c.setAutoCommit(oldAutoCommit);
                        return id;
                    }
                }
                c.rollback();
                c.setAutoCommit(oldAutoCommit);
                return -1;
            } catch (Exception inner) {
                c.rollback();
                c.setAutoCommit(oldAutoCommit);
                throw inner;
            }
        } catch (Exception e) {
            throw new IllegalStateException("save reservation failed: " + e.getMessage(), e);
        }
    }

    private void saveReservationServices(Connection c, int reservationId, Reservation reservation, List<Service> services) throws Exception {
        if (services == null || services.isEmpty()) return;
        long nights = java.time.temporal.ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate());
        String sql = """
            INSERT INTO reservation_services (reservation_id, service_code, service_name, unit_price, quantity, billing_type, line_total)
            VALUES (?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Service svc : services) {
                String code = svc.getName().toUpperCase();
                String billingType = "PER_NIGHT";
                int quantity = 1;
                if (svc instanceof SelectedExtraService ses) {
                    code = ses.getCode();
                    billingType = ses.getBillingType();
                    quantity = ses.getQuantity();
                }
                double lineTotal = "PER_STAY".equalsIgnoreCase(billingType)
                    ? (svc.getPrice() * quantity)
                    : (svc.getPrice() * nights * quantity);
                ps.setInt(1, reservationId);
                ps.setString(2, code);
                ps.setString(3, svc.getName());
                ps.setDouble(4, svc.getPrice());
                ps.setInt(5, quantity);
                ps.setString(6, billingType);
                ps.setDouble(7, lineTotal);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public List<Reservation> findByCustomerId(int customerId) {
        return queryReservations("WHERE r.customer_id=? ORDER BY r.id DESC", ps -> ps.setInt(1, customerId));
    }

    @Override
    public List<Reservation> findAll() {
        return queryReservations("ORDER BY r.id", ps -> {});
    }

    private List<Reservation> queryReservations(String suffix, ThrowingConsumer<PreparedStatement> binder) {
        String sql = """
            SELECT r.id, r.customer_id, r.room_id, r.check_in, r.check_out, r.total_price, r.status, r.created_at,
                   u.full_name AS customer_name,
                   rm.room_number AS room_number,
                   rm.room_type AS room_type_enum
            FROM reservations r
            JOIN users u ON r.customer_id = u.id
            JOIN rooms rm ON r.room_id = rm.id
            """ + suffix;
        List<Reservation> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapReservationRow(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("query reservations failed: " + e.getMessage(), e);
        }
        return list;
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    private static Reservation mapReservationRow(ResultSet rs) throws Exception {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setCustomerId(rs.getInt("customer_id"));
        r.setRoomId(rs.getInt("room_id"));
        r.setCheckInDate(rs.getObject("check_in", LocalDate.class));
        r.setCheckOutDate(rs.getObject("check_out", LocalDate.class));
        r.setTotalPrice(rs.getDouble("total_price"));
        r.setStatus(Status.valueOf(rs.getString("status")));
        r.setCreatedAt(rs.getString("created_at"));
        r.setCustomerName(rs.getString("customer_name"));
        r.setRoomNumber(rs.getString("room_number"));
        String rte = rs.getString("room_type_enum");
        if (rte != null) {
            try {
                Room tmp = new Room();
                tmp.setRoomType(Room.RoomType.valueOf(rte));
                r.setRoomType(tmp.getRoomTypeDisplay());
            } catch (Exception e) {
                r.setRoomType(rte);
            }
        }
        return r;
    }

    @Override
    public boolean updateStatus(int reservationId, Status status) {
        String sql = "UPDATE reservations SET status=? WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, reservationId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException("update status failed: " + e.getMessage(), e);
        }
    }

    public double getTotalRevenue() {
        String sql = "SELECT COALESCE(SUM(total_price),0) FROM reservations WHERE status='APPROVED'";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("getTotalRevenue failed: " + e.getMessage(), e);
        }
    }

    private boolean isRoomAvailableForReservation(Reservation reservation) {
        String sql = """
            SELECT COUNT(*) FROM reservations res
            WHERE res.room_id = ?
            AND res.status <> 'CANCELLED'
            AND res.check_in < ?
            AND res.check_out > ?
            """;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservation.getRoomId());
            ps.setObject(2, reservation.getCheckOutDate());
            ps.setObject(3, reservation.getCheckInDate());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
