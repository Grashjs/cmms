package com.grash.service;

import com.grash.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
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

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailService2 {

    private final JavaMailSender emailSender;

    private final SimpleMailMessage template;
    private final MailProperties mailProperties;
    @Value("${spring.mail.username:#{null}")
    private String smtpUsername;

    @Value("${mail.enable}")
    private Boolean enableEmails;

    @Value("${spring.mail.password:#{null}}")
    private String smtpPassword;

    private final SpringTemplateEngine thymeleafTemplateEngine;

    @Value("classpath:/static/images/logo.png")
    private Resource resourceFile;

    private final Environment environment;


    public void sendSimpleMessage(String[] to, String subject, String text) {
        if (Boolean.FALSE.equals(enableEmails))
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

    public void sendMessageWithAttachment(String to,
                                          String subject,
                                          String text,
                                          String pathToAttachment) {
        if (Boolean.FALSE.equals(enableEmails))
            return;
        try {
            MimeMessage message = emailSender.createMimeMessage();
            // pass 'true' to the constructor to create a multipart message
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            FileSystemResource file = new FileSystemResource(new File(pathToAttachment));
            helper.addAttachment("Invoice", file);

            emailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


    @Async
    public void sendMessageUsingThymeleafTemplate(
            String[] to, String subject, Map<String, Object> templateModel, String template, Locale locale) {
        if (Boolean.FALSE.equals(enableEmails))
            return;
        Context thymeleafContext = new Context();
        thymeleafContext.setLocale(locale);
        thymeleafContext.setVariables(templateModel);
        thymeleafContext.setVariable("environment", environment);
        String htmlBody = thymeleafTemplateEngine.process(template, thymeleafContext);

        try {
            sendHtmlMessage(to, subject, htmlBody);
        } catch (MessagingException e) {
            throw new CustomException("Can't send the mail", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public void sendHtmlMessage(String[] to, String subject, String htmlBody) throws MessagingException {
        if (Boolean.FALSE.equals(enableEmails))
            return;
        if (to.length > 0) {

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            try {
                helper.setFrom(new InternetAddress(mailProperties.getUsername(), "Atlas CMMS"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            //helper.addInline("attachment.png", resourceFile);
            emailSender.send(message);
        }
    }

}
