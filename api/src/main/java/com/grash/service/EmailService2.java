package com.grash.service;

import com.grash.dto.EmailAttachmentDTO;
import com.grash.exception.CustomException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailService2 implements MailService {

    private final JavaMailSender emailSender;

    private final SimpleMailMessage template;
    private final MailProperties mailProperties;
    private final BrandingService brandingService;
    @Value("${spring.mail.username:#{null}}")
    private String smtpUsername;

    @Value("${mail.enable}")
    private Boolean enableEmails;

    @Value("${spring.mail.password:#{null}}")
    private String smtpPassword;

    private final SpringTemplateEngine thymeleafTemplateEngine;

    @Value("classpath:/static/images/logo.png")
    private Resource resourceFile;

    @Value("${mail.recipients}")
    private String[] recipients;

    private final Environment environment;


    @Override
    public void sendSimpleMessage(String[] to, String subject, String text) {
        if (shouldSkipSendingMail())
            return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            emailSender.send(message);
        } catch (MailException exception) {
            exception.printStackTrace();
        }
    }

    private boolean shouldSkipSendingMail() {
        return Boolean.FALSE.equals(enableEmails) || MailService.skipMail.get();
    }

    @Override
    public void sendMessageWithAttachment(String to,
                                          String subject,
                                          String text,
                                          String attachmentName,
                                          byte[] attachmentData,
                                          String attachmentType) {
        if (shouldSkipSendingMail())
            return;
        try {
            MimeMessage message = emailSender.createMimeMessage();
            // pass 'true' to the constructor to create a multipart message
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            helper.addAttachment(attachmentName, new ByteArrayDataSource(attachmentData, attachmentType));

            emailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


    @Override
    @Async
    public void sendMessageUsingThymeleafTemplate(
            String[] to, String subject, Map<String, Object> templateModel, String template, Locale locale,
            List<EmailAttachmentDTO> attachmentDTOS) {
        if (shouldSkipSendingMail())
            return;
        Context thymeleafContext = new Context();
        thymeleafContext.setLocale(locale);
        thymeleafContext.setVariables(templateModel);
        thymeleafContext.setVariable("environment", environment);
        thymeleafContext.setVariable("brandConfig", brandingService.getBrandConfig());
        thymeleafContext.setVariable("backgroundColor", brandingService.getMailBackgroundColor());
        String htmlBody = thymeleafTemplateEngine.process(template, thymeleafContext);

        try {
            sendHtmlMessage(to, subject, htmlBody, attachmentDTOS);
        } catch (MessagingException | IOException e) {
            throw new CustomException("Can't send the mail", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public void sendHtmlMessage(String[] to, String subject, String htmlBody,
                                List<EmailAttachmentDTO> attachmentDTOS) throws MessagingException, IOException {
        if (shouldSkipSendingMail())
            return;
        if (to.length > 0) {

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            try {
                helper.setFrom(new InternetAddress(mailProperties.getUsername(),
                        brandingService.getBrandConfig().getName()));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (attachmentDTOS != null) {
                for (EmailAttachmentDTO attachmentDTO : attachmentDTOS) {
                    helper.addAttachment(attachmentDTO.getAttachmentName(),
                            new ByteArrayDataSource(attachmentDTO.getAttachmentData(),
                                    attachmentDTO.getAttachmentType()));
                }
            }

            //helper.addInline("attachment.png", resourceFile);
            emailSender.send(message);
        }
    }

    @Override
    public void sendMailToSuperAdmins(String subject, String text) {
        try {
            sendHtmlMessage(recipients, subject, text, null);
        } catch (MessagingException | IOException e) {
            throw new CustomException("Failed to send email to super admins",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Async
    public void removeUserFromContactList(String userEmail) {
        throw new RuntimeException("Not implemented");
    }
}
