package com.grash.service;

import com.grash.dto.EmailAttachmentDTO;
import jakarta.mail.MessagingException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface MailService {
    ThreadLocal<Boolean> skipMail = ThreadLocal.withInitial(() -> false);

    void sendSimpleMessage(String[] to, String subject, String text);

    void sendMessageWithAttachment(String to, String subject, String text, String attachmentName,
                                   byte[] attachmentData, String attachmentType);

    void sendMessageUsingThymeleafTemplate(String[] to, String subject, Map<String, Object> templateModel,
                                           String template, Locale locale, List<EmailAttachmentDTO> attachmentDTOS);

    default void sendMessageUsingThymeleafTemplate(String[] to, String subject, Map<String, Object> templateModel,
                                                   String template, Locale locale) {
        sendMessageUsingThymeleafTemplate(to, subject, templateModel, template, locale, null);
    }

    void sendHtmlMessage(String[] to, String subject, String htmlBody, List<EmailAttachmentDTO> attachmentDTOS) throws MessagingException, java.io.IOException;

    default void sendHtmlMessage(String[] to, String subject, String htmlBody) throws MessagingException, IOException {
        sendHtmlMessage(to, subject, htmlBody, null);
    }

    void sendMailToSuperAdmins(String subject, String text);
}
