package com.hotel.service;

import com.hotel.database.dao.IUserDAO;
import com.hotel.database.dao.UserDAO;
import com.hotel.model.Customer;
import com.hotel.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class AuthService {

    private final IUserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public Optional<User> login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        Optional<User> userOpt = userDAO.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) return Optional.empty();

        User user = userOpt.get();
        if (BCrypt.checkpw(password, user.getPassword())) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public String register(String fullName, String email, String password, String phone) {
        if (fullName == null || fullName.isBlank())
            return "Full name cannot be empty.";
        if (email == null || email.isBlank())
            return "Email cannot be empty.";
        if (!isValidEmail(email))
            return "Invalid email format.";
        if (password == null || password.length() < 6)
            return "Password must be at least 6 characters.";
        if (phone == null || phone.isBlank())
            return "Phone number cannot be empty.";

        String normalizedEmail = email.trim().toLowerCase();

        if (userDAO.emailExists(normalizedEmail))
            return "This email address is already registered.";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(10));
        Customer customer = new Customer(fullName.trim(), normalizedEmail, hashedPassword, phone.trim());

        boolean saved = userDAO.save(customer);
        return saved ? "OK" : "An error occurred during registration. Please try again.";
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }
}
