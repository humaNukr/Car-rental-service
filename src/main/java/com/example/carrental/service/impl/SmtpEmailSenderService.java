package com.example.carrental.service.impl;

import com.example.carrental.properties.MailProperties;
import com.example.carrental.service.interfaces.EmailSenderService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailSenderService implements EmailSenderService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;

    @Async
    @Override
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> templateModel) {
        try {
            Context thymeleafContext = new Context();
            thymeleafContext.setVariables(templateModel);

            String htmlBody = templateEngine.process(templateName, thymeleafContext);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.getUsername());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
        }
    }
}