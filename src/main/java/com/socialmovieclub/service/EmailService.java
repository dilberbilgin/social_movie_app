package com.socialmovieclub.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

@Service
@RequiredArgsConstructor
@Slf4j

public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Thymeleaf için

    @Async
    public void sendHtmlMail(String to, String subject, String title, String message, String actionUrl, String buttonText) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Thymeleaf context'ini hazırla
            Context context = new Context();
            context.setVariable("title", title);
            context.setVariable("message", message);
            context.setVariable("actionUrl", actionUrl);
            context.setVariable("buttonText", buttonText);

            // HTML'i işle
            String htmlContent = templateEngine.process("notification-email", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML gönderimi
            helper.setFrom("noreply@socialmovieclub.com");

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
        }
    }
}
