package com.grash.service;

import com.grash.dto.BrandConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring5.SpringTemplateEngine;

import org.springframework.test.util.ReflectionTestUtils;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailService2Test {

    @InjectMocks
    private EmailService2 emailService;

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private SpringTemplateEngine thymeleafTemplateEngine;

    @Mock
    private BrandingService brandingService;

    @Mock
    private Environment environment;

    @Mock
    private MailProperties mailProperties;

    @Mock
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        lenient().when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Nested
    @DisplayName("Send Simple Message Tests")
    class SendSimpleMessageTests {

        @Test
        @DisplayName("Should send simple message when emails are enabled")
        void shouldSendSimpleMessageWhenEmailsAreEnabled() {
            setEnableEmails(true);
            String[] to = {"test@example.com"};
            String subject = "Test Subject";
            String text = "Test Text";

            emailService.sendSimpleMessage(to, subject, text);

            verify(emailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("Should not send simple message when emails are disabled")
        void shouldNotSendSimpleMessageWhenEmailsAreDisabled() {
            setEnableEmails(false);
            String[] to = {"test@example.com"};
            String subject = "Test Subject";
            String text = "Test Text";

            emailService.sendSimpleMessage(to, subject, text);

            verify(emailSender, never()).send(any(SimpleMailMessage.class));
        }
    }

    @Nested
    @DisplayName("Send Message With Attachment Tests")
    class SendMessageWithAttachmentTests {

        @Test
        @DisplayName("Should send message with attachment when emails are enabled")
        void shouldSendMessageWithAttachmentWhenEmailsAreEnabled() {
            setEnableEmails(true);
            String to = "test@example.com";
            String subject = "Test Subject";
            String text = "Test Text";
            String pathToAttachment = "/test/path/to/attachment.txt";

            emailService.sendMessageWithAttachment(to, subject, text, pathToAttachment);

            verify(emailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send message with attachment when emails are disabled")
        void shouldNotSendMessageWithAttachmentWhenEmailsAreDisabled() {
            setEnableEmails(false);
            String to = "test@example.com";
            String subject = "Test Subject";
            String text = "Test Text";
            String pathToAttachment = "/test/path/to/attachment.txt";

            emailService.sendMessageWithAttachment(to, subject, text, pathToAttachment);

            verify(emailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Send Message Using Thymeleaf Template Tests")
    class SendMessageUsingThymeleafTemplateTests {

        @Test
        @DisplayName("Should send message using thymeleaf template when emails are enabled")
        void shouldSendMessageUsingThymeleafTemplateWhenEmailsAreEnabled() throws Exception {
            setEnableEmails(true);
            String[] to = {"test@example.com"};
            String subject = "Test Subject";
            Map<String, Object> templateModel = new HashMap<>();
            String template = "test-template";
            Locale locale = Locale.ENGLISH;

            BrandConfig brandConfig = new BrandConfig();
            brandConfig.setName("Test Brand");

            when(brandingService.getBrandConfig()).thenReturn(brandConfig);
            when(thymeleafTemplateEngine.process(anyString(), any())).thenReturn("html-body");

            emailService.sendMessageUsingThymeleafTemplate(to, subject, templateModel, template, locale);

            verify(emailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send message using thymeleaf template when emails are disabled")
        void shouldNotSendMessageUsingThymeleafTemplateWhenEmailsAreDisabled() {
            setEnableEmails(false);
            String[] to = {"test@example.com"};
            String subject = "Test Subject";
            Map<String, Object> templateModel = new HashMap<>();
            String template = "test-template";
            Locale locale = Locale.ENGLISH;

            emailService.sendMessageUsingThymeleafTemplate(to, subject, templateModel, template, locale);

            verify(emailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Send HTML Message Tests")
    class SendHtmlMessageTests {

        @Test
        @DisplayName("Should send html message when emails are enabled")
        void shouldSendHtmlMessageWhenEmailsAreEnabled() throws Exception {
            setEnableEmails(true);
            String[] to = {"test@example.com"};
            String subject = "Test Subject";
            String htmlBody = "<html><body>Test</body></html>";

            BrandConfig brandConfig = new BrandConfig();
            brandConfig.setName("Test Brand");

            when(brandingService.getBrandConfig()).thenReturn(brandConfig);

            emailService.sendHtmlMessage(to, subject, htmlBody);

            verify(emailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send html message when emails are disabled")
        void shouldNotSendHtmlMessageWhenEmailsAreDisabled() throws Exception {
            setEnableEmails(false);
            String[] to = {"test@example.com"};
            String subject = "Test Subject";
            String htmlBody = "<html><body>Test</body></html>";

            emailService.sendHtmlMessage(to, subject, htmlBody);

            verify(emailSender, never()).send(any(MimeMessage.class));
        }
    }

    private void setEnableEmails(boolean enabled) {
        ReflectionTestUtils.setField(emailService, "enableEmails", enabled);
    }
}
