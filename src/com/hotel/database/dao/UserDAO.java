package com.hotel.database.dao;

import com.hotel.database.DatabaseConnection;
import com.hotel.model.User;

import java.util.Optional;

public class UserDAO implements IUserDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public Optional<User> findByEmail(String email) {
        return db.users().stream()
            .filter(u -> u.getEmail().equalsIgnoreCase(email))
            .findFirst();
    }

    @Override
    public boolean emailExists(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public boolean save(User user) {
        if (emailExists(user.getEmail())) return false;
        user.setId(db.nextUserId());
        boolean added = db.users().add(user);
        if (added) db.persistAll();
        return added;
    }
}
