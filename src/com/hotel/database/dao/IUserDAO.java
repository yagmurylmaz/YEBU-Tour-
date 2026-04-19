package com.hotel.database.dao;

import com.hotel.model.User;

import java.util.Optional;

public interface IUserDAO {
    Optional<User> findByEmail(String email);
    boolean emailExists(String email);
    boolean save(User user);
    boolean updatePasswordHash(int userId, String passwordHash);
}
