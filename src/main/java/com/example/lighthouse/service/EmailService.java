package com.example.lighthouse.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Properties;

@Service
public class EmailService {

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.from:noreply@lighthouse.ai}")
    private String mailFrom;

    private JavaMailSender mailSender;
    private JavaMailSenderImpl mailSenderInstance; // Cache the instance

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private JavaMailSender getMailSender() {
        if (mailSender != null) {
            return mailSender;
        }

        // Return cached instance if available
        if (mailSenderInstance != null) {
            return mailSenderInstance;
        }

        // Only create mail sender if configured
        if (mailHost == null || mailHost.isEmpty() ||
                mailUsername == null || mailUsername.isEmpty()) {
            System.out.println("⚠️ Email not configured - missing host or username");
            return null;
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailHost);
        sender.setPort(mailPort);
        sender.setUsername(mailUsername);
        sender.setPassword(mailPassword);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        mailSenderInstance = sender; // Cache it
        System.out.println("✅ EmailService initialized with host: " + mailHost);
        return sender;
    }

    public void sendHallucinationAlert(String userEmail, String traceId, String prompt,
                                       String response, double confidenceScore,
                                       int unsupportedClaimsCount) {
        JavaMailSender sender = getMailSender();

        if (sender == null) {
            System.out.println("⚠️ Email not configured - skipping hallucination alert");
            return;
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            System.out.println("⚠️ No recipient email provided - skipping hallucination alert");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(userEmail);
            message.setSubject("⚠️ Hallucination Detected in AI Query");

            String body = String.format(
                    "A hallucination has been detected in one of your AI queries.\n\n" +
                            "Confidence Score: %.1f%%\n" +
                            "Unsupported Claims: %d\n\n" +
                            "Query Prompt:\n%s\n\n" +
                            "AI Response:\n%s\n\n" +
                            "View details in your Lighthouse dashboard:\n" +
                            "http://localhost:5173/#dashboard\n\n" +
                            "Trace ID: %s",
                    confidenceScore,
                    unsupportedClaimsCount,
                    prompt.length() > 200 ? prompt.substring(0, 200) + "..." : prompt,
                    response.length() > 300 ? response.substring(0, 300) + "..." : response,
                    traceId
            );

            message.setText(body);
            sender.send(message);
            System.out.println("✅ Hallucination alert email sent to: " + userEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send hallucination alert email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}