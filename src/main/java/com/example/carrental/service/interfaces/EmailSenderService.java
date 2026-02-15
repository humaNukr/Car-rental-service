package com.example.carrental.service.interfaces;

public interface EmailSenderService {
    /**
     * Sends an HTML email using a template.
     * @param to Recipient's email address
     * @param subject Email subject line
     * @param templateName Name of the HTML template file (e.g., "payment-success")
     * @param templateModel Variables to be substituted into the template
     */
    void sendHtmlEmail(String to, String subject, String templateName, java.util.Map<String, Object> templateModel);
}