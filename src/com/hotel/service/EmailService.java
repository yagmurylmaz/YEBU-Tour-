package com.hotel.service;

import com.hotel.config.MailConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    private final Properties mailProps;

    public EmailService() {
        this.mailProps = MailConfig.load();
    }

    public void sendPasswordResetCode(String toEmail, String recipientName, String plainToken) throws Exception {
        if (!MailConfig.isSmtpConfigured(mailProps)) {
            throw new IllegalStateException("SMTP is not configured.");
        }
        String host = mailProps.getProperty("mail.smtp.host").trim();
        int port = Integer.parseInt(mailProps.getProperty("mail.smtp.port", "587").trim());
        boolean auth = Boolean.parseBoolean(mailProps.getProperty("mail.smtp.auth", "true"));
        boolean startTls = Boolean.parseBoolean(mailProps.getProperty("mail.smtp.starttls.enable", "true"));
        String user = mailProps.getProperty("mail.user", "").trim();
        String password = mailProps.getProperty("mail.password", "");
        String from = mailProps.getProperty("mail.from").trim();
        String fromName = mailProps.getProperty("mail.from.name", "YEBU Tour").trim();

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTls));

        // Optional keys from mail.properties (timeouts, TLS)
        copySmtpOption(mailProps, props, "mail.smtp.starttls.required");
        copySmtpOption(mailProps, props, "mail.smtp.ssl.protocols");
        copySmtpOption(mailProps, props, "mail.smtp.ssl.trust");
        copySmtpOption(mailProps, props, "mail.smtp.connectiontimeout");
        copySmtpOption(mailProps, props, "mail.smtp.timeout");
        copySmtpOption(mailProps, props, "mail.smtp.writetimeout");

        // Gmail / many providers: explicit TLS + trust host avoids handshake issues on some JDKs
        if (host.toLowerCase().contains("gmail.com")) {
            props.putIfAbsent("mail.smtp.starttls.required", "true");
            props.putIfAbsent("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.putIfAbsent("mail.smtp.ssl.trust", host);
        }
        props.putIfAbsent("mail.smtp.connectiontimeout", "15000");
        props.putIfAbsent("mail.smtp.timeout", "15000");

        boolean debug = Boolean.parseBoolean(mailProps.getProperty("mail.debug", "false"));
        props.put("mail.debug", String.valueOf(debug));

        Session session = Session.getInstance(props, auth ? new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        } : null);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from, fromName, "UTF-8"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        message.setSubject("Reset your YEBU Tour password", "UTF-8");

        String greeting = (recipientName != null && !recipientName.isBlank()) ? recipientName.trim() : "there";
        String body = """
            Hello %s,

            We received a request to reset the password for your YEBU Tour account.

            Your password reset code is (copy the whole line; line breaks in the email are OK):

            %s

            Open the application, go to "Reset password", paste the code, and enter your new password. This code expires in 1 hour.

            If you did not request a password reset, you can safely ignore this email.

            Best regards,
            YEBU Tour
            """.formatted(greeting, plainToken);

        message.setText(body, "UTF-8");

        Transport.send(message);
    }

    private static void copySmtpOption(Properties from, Properties to, String key) {
        String v = from.getProperty(key);
        if (v != null && !v.isBlank()) {
            to.setProperty(key, v.trim());
        }
    }
}
