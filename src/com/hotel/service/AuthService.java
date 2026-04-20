package com.hotel.service;

import com.hotel.database.dao.IUserDAO;
import com.hotel.database.dao.RegistrationVerificationDAO;
import com.hotel.database.dao.UserDAO;
import com.hotel.model.Customer;
import com.hotel.model.User;
import com.hotel.config.MailConfig;
import org.mindrot.jbcrypt.BCrypt;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AuthService {
    private static final int REG_CODE_VALID_MINUTES = 10;

    private final IUserDAO userDAO;
    private final RegistrationVerificationDAO registrationVerificationDAO;
    private final EmailService emailService;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.registrationVerificationDAO = new RegistrationVerificationDAO();
        this.emailService = new EmailService();
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

    public Optional<User> findByEmailForRememberMe(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return userDAO.findByEmail(email.trim().toLowerCase());
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

    public String requestRegistrationVerificationCode(String fullName, String email, String password, String phone) {
        String validation = validateRegistrationInput(fullName, email, password, phone);
        if (validation != null) return validation;

        Properties mailProps = MailConfig.load();
        if (!MailConfig.isSmtpConfigured(mailProps)) {
            String hint = MailConfig.smtpMissingHint(mailProps);
            return hint.isEmpty()
                ? "Email sending is not configured."
                : hint;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userDAO.emailExists(normalizedEmail)) {
            return "This email address is already registered.";
        }

        String code = generateNumericCode(6);
        String codeHash = sha256Hex(code);
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        Instant expiresAt = Instant.now().plus(REG_CODE_VALID_MINUTES, ChronoUnit.MINUTES);

        registrationVerificationDAO.replacePending(
            normalizedEmail,
            fullName.trim(),
            phone.trim(),
            passwordHash,
            codeHash,
            expiresAt
        );
        try {
            emailService.sendRegistrationVerificationCode(normalizedEmail, fullName.trim(), code);
            return "OK";
        } catch (Exception e) {
            System.err.println("[RegisterVerification] Email send failed:");
            e.printStackTrace();
            return "We could not send verification email. Check SMTP settings.";
        }
    }

    public String verifyRegistrationCodeAndCreateUser(String email, String code) {
        if (email == null || email.isBlank()) return "Email cannot be empty.";
        if (code == null || code.isBlank()) return "Verification code cannot be empty.";

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userDAO.emailExists(normalizedEmail)) {
            registrationVerificationDAO.deleteByEmail(normalizedEmail);
            return "This email address is already registered.";
        }

        String normalizedCode = normalizeVerificationCode(code);
        if (normalizedCode.length() != 6) return "Verification code must be 6 digits.";

        var pendingOpt = registrationVerificationDAO.findValidByEmailAndCodeHash(normalizedEmail, sha256Hex(normalizedCode));
        if (pendingOpt.isEmpty()) {
            return "Verification code is invalid or expired. Please request a new code.";
        }

        RegistrationVerificationDAO.PendingRegistration pending = pendingOpt.get();
        Customer customer = new Customer(pending.fullName(), pending.email(), pending.passwordHash(), pending.phone());
        boolean saved = userDAO.save(customer);
        if (!saved) {
            return "Registration failed. Please try again.";
        }
        registrationVerificationDAO.deleteByEmail(normalizedEmail);
        return "OK";
    }

    private String validateRegistrationInput(String fullName, String email, String password, String phone) {
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
        return null;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }

    private static String generateNumericCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private static String normalizeVerificationCode(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\D+", "");
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
