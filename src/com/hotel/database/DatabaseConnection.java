package com.hotel.database;

import com.hotel.model.Reservation;
import com.hotel.model.Room;
import com.hotel.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseConnection {
    private static DatabaseConnection instance;

    private final List<User> users = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();

    private final AtomicInteger userIdSeq = new AtomicInteger(0);
    private final AtomicInteger roomIdSeq = new AtomicInteger(0);
    private final AtomicInteger reservationIdSeq = new AtomicInteger(0);
    private final Path dataDir = Path.of("data");
    private final Path usersFile = dataDir.resolve("users.csv");
    private final Path roomsFile = dataDir.resolve("rooms.csv");
    private final Path reservationsFile = dataDir.resolve("reservations.csv");

    private DatabaseConnection() {
        loadFromDisk();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) instance = new DatabaseConnection();
        return instance;
    }

    public List<User> users() { return users; }
    public List<Room> rooms() { return rooms; }
    public List<Reservation> reservations() { return reservations; }

    public int nextUserId() { return userIdSeq.incrementAndGet(); }
    public int nextRoomId() { return roomIdSeq.incrementAndGet(); }
    public int nextReservationId() { return reservationIdSeq.incrementAndGet(); }

    public synchronized void persistAll() {
        try {
            Files.createDirectories(dataDir);
            saveUsers();
            saveRooms();
            saveReservations();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save data: " + e.getMessage(), e);
        }
    }

    public boolean hasAnyData() {
        return !users.isEmpty() || !rooms.isEmpty() || !reservations.isEmpty();
    }

    public synchronized void loadFromDisk() {
        users.clear();
        rooms.clear();
        reservations.clear();
        userIdSeq.set(0);
        roomIdSeq.set(0);
        reservationIdSeq.set(0);
        try {
            Files.createDirectories(dataDir);
            loadUsers();
            loadRooms();
            loadReservations();
            resetSequences();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read data files: " + e.getMessage(), e);
        }
    }

    public void closeConnection() {
        persistAll();
    }

    private void loadUsers() throws IOException {
        if (!Files.exists(usersFile)) return;
        for (String line : Files.readAllLines(usersFile)) {
            if (line.isBlank()) continue;
            String[] p = line.split("\t", -1);
            if (p.length < 6) continue;
            User user = new User();
            user.setId(Integer.parseInt(p[0]));
            user.setFullName(unescape(p[1]));
            user.setEmail(unescape(p[2]));
            user.setPassword(unescape(p[3]));
            user.setPhone(unescape(p[4]));
            user.setRole(unescape(p[5]));
            users.add(user);
        }
    }

    private void loadRooms() throws IOException {
        if (!Files.exists(roomsFile)) return;
        for (String line : Files.readAllLines(roomsFile)) {
            if (line.isBlank()) continue;
            String[] p = line.split("\t", -1);
            if (p.length < 7) continue;
            Room room = new Room();
            room.setId(Integer.parseInt(p[0]));
            room.setRoomNumber(unescape(p[1]));
            room.setRoomType(Room.RoomType.valueOf(p[2]));
            room.setPricePerNight(Double.parseDouble(p[3]));
            room.setCapacity(Integer.parseInt(p[4]));
            room.setDescription(unescape(p[5]));
            room.setAvailable(Boolean.parseBoolean(p[6]));
            rooms.add(room);
        }
    }

    private void loadReservations() throws IOException {
        if (!Files.exists(reservationsFile)) return;
        for (String line : Files.readAllLines(reservationsFile)) {
            if (line.isBlank()) continue;
            String[] p = line.split("\t", -1);
            if (p.length < 8) continue;
            Reservation reservation = new Reservation();
            reservation.setId(Integer.parseInt(p[0]));
            reservation.setCustomerId(Integer.parseInt(p[1]));
            reservation.setRoomId(Integer.parseInt(p[2]));
            reservation.setCheckInDate(java.time.LocalDate.parse(p[3]));
            reservation.setCheckOutDate(java.time.LocalDate.parse(p[4]));
            reservation.setTotalPrice(Double.parseDouble(p[5]));
            reservation.setStatus(Reservation.Status.valueOf(p[6]));
            reservation.setCreatedAt(unescape(p[7]));
            reservations.add(reservation);
        }
    }

    private void saveUsers() throws IOException {
        List<String> lines = new ArrayList<>();
        for (User user : users) {
            lines.add(user.getId() + "\t" +
                escape(user.getFullName()) + "\t" +
                escape(user.getEmail()) + "\t" +
                escape(user.getPassword()) + "\t" +
                escape(user.getPhone()) + "\t" +
                escape(user.getRole()));
        }
        Files.write(usersFile, lines);
    }

    private void saveRooms() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Room room : rooms) {
            lines.add(room.getId() + "\t" +
                escape(room.getRoomNumber()) + "\t" +
                room.getRoomType().name() + "\t" +
                room.getPricePerNight() + "\t" +
                room.getCapacity() + "\t" +
                escape(room.getDescription()) + "\t" +
                room.isAvailable());
        }
        Files.write(roomsFile, lines);
    }

    private void saveReservations() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Reservation r : reservations) {
            lines.add(r.getId() + "\t" +
                r.getCustomerId() + "\t" +
                r.getRoomId() + "\t" +
                r.getCheckInDate() + "\t" +
                r.getCheckOutDate() + "\t" +
                r.getTotalPrice() + "\t" +
                r.getStatus().name() + "\t" +
                escape(r.getCreatedAt()));
        }
        Files.write(reservationsFile, lines);
    }

    private void resetSequences() {
        for (User user : users) userIdSeq.set(Math.max(userIdSeq.get(), user.getId()));
        for (Room room : rooms) roomIdSeq.set(Math.max(roomIdSeq.get(), room.getId()));
        for (Reservation reservation : reservations) {
            reservationIdSeq.set(Math.max(reservationIdSeq.get(), reservation.getId()));
        }
    }

    private String escape(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\n", "\\n")
            .replace("\r", "");
    }

    private String unescape(String encoded) {
        return encoded.replace("\\t", "\t")
            .replace("\\n", "\n")
            .replace("\\\\", "\\");
    }
}
