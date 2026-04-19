package com.hotel.service;

import com.hotel.config.MailConfig;
import com.hotel.database.dao.IUserDAO;
import com.hotel.database.dao.PasswordResetDAO;
import com.hotel.database.dao.UserDAO;
import com.hotel.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public class PasswordResetService {

    private static final int TOKEN_BYTES = 24;
    private static final int TOKEN_VALID_HOURS = 1;

    private final IUserDAO userDAO = new UserDAO();
    private final PasswordResetDAO resetDAO = new PasswordResetDAO();
    private final EmailService emailService = new EmailService();

    /**
     * @return user-facing message in English
     */
    public String requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return "Please enter your email address.";
        }
        Properties mailProps = MailConfig.load();
        if (!MailConfig.isSmtpConfigured(mailProps)) {
            String hint = MailConfig.smtpMissingHint(mailProps);
            return hint.isEmpty()
                ? "Password reset email is not available: check mail.properties."
                : hint;
        }

        String normalized = email.trim().toLowerCase();
        Optional<User> userOpt = userDAO.findByEmail(normalized);

        if (userOpt.isEmpty()) {
            return genericSentMessage();
        }

        User user = userOpt.get();
        String plainToken = generateToken();
        String tokenHash = sha256Hex(plainToken);
        Instant expires = Instant.now().plus(TOKEN_VALID_HOURS, ChronoUnit.HOURS);
        resetDAO.replaceToken(user.getId(), tokenHash, expires);

        try {
            emailService.sendPasswordResetCode(user.getEmail(), user.getFullName(), plainToken);
        } catch (Exception e) {
            System.err.println("[PasswordReset] Email send failed:");
            e.printStackTrace();
            return "We could not send the reset email. " + mailFailureHint(e);
        }
        return genericSentMessage();
    }

    /**
     * @return user-facing message in English
     */
    public String resetPassword(String token, String newPassword, String confirmPassword) {
        if (token == null || token.isBlank()) {
            return "Please enter the reset code from your email.";
        }
        if (newPassword == null || newPassword.length() < 6) {
            return "Password must be at least 6 characters.";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "Passwords do not match.";
        }

        String plain = normalizeResetCode(token);
        if (plain.isEmpty()) {
            return "Please enter the reset code from your email.";
        }
        String tokenHash = sha256Hex(plain);
        Optional<Integer> userIdOpt = resetDAO.findValidUserIdByTokenHash(tokenHash);
        if (userIdOpt.isEmpty()) {
            return "This reset code is invalid or has expired. Request a new one from Forgot password.";
        }

        int userId = userIdOpt.get();
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
        boolean ok = userDAO.updatePasswordHash(userId, hashed);
        if (!ok) {
            return "Could not update your password. Please try again.";
        }
        resetDAO.deleteByUserId(userId);
        return "OK";
    }

    private static String genericSentMessage() {
        return "If an account exists for that email address, you will receive password reset instructions shortly.";
    }

    /** Short English hint for the UI; full details go to stderr. */
    private static String mailFailureHint(Throwable e) {
        Throwable t = e;
        while (t != null) {
            String m = t.getMessage() != null ? t.getMessage() : "";
            String cn = t.getClass().getSimpleName();
            if (cn.contains("AuthenticationFailed") || m.contains("535") || m.contains("534")
                || m.contains("Username and Password not accepted")) {
                return "SMTP login failed. For Gmail use an App Password (Google Account → Security → 2-Step Verification → App passwords), not your normal password.";
            }
            if (m.contains("Could not connect to SMTP host") || m.contains("Connection refused")
                || m.contains("Connection timed out") || m.contains("timed out")) {
                return "Cannot reach the mail server. Check mail.smtp.host/port, firewall, and network.";
            }
            if (m.contains("PKIX") || m.contains("certificate") || m.contains("SSL") || m.contains("handshake")) {
                return "TLS/SSL error. Try mail.smtp.ssl.trust=smtp.gmail.com in mail.properties or update Java.";
            }
            t = t.getCause();
        }
        return "Check the terminal output for details.";
    }

    /**
     * Email clients often insert line breaks or spaces in the middle of the hex code.
     * Generated tokens are lowercase hex ({@link HexFormat#formatHex}); input is normalized the same way.
     */
    private static String normalizeResetCode(String raw) {
        if (raw == null) {
            return "";
        }
        String noWs = raw.replaceAll("\\s+", "");
        String hexOnly = noWs.replaceAll("[^0-9a-fA-F]", "");
        return hexOnly.toLowerCase(Locale.ROOT);
    }

    private static String generateToken() {
        byte[] buf = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(buf);
        return HexFormat.of().formatHex(buf);
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
