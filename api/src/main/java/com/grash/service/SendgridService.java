package com.grash.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.EmailAttachmentDTO;
import com.grash.event.CompanyCreatedEvent;
import com.grash.exception.CustomException;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import jakarta.transaction.Transactional;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SendgridService implements MailService {

    private final SpringTemplateEngine thymeleafTemplateEngine;
    private final BrandingService brandingService;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    private String fromName;

    @Value("classpath:/static/images/logo.png")
    private Resource resourceFile;

    @Value("${api.host}")
    private String API_HOST;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${mail.recipients}")
    private String[] recipients;
    @Value("${mail.enable}")
    private Boolean enableEmails;
    @Value("${cloud-version}")
    private boolean cloudVersion;
    @Value("${sendgrid.contact-list-id}")
    private String contactListId;

    @PostConstruct
    public void init() {
        fromName = brandingService.getBrandConfig().getName();
    }

    /**
     * Send simple text email
     */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleUserCreated(CompanyCreatedEvent event) {
        if (shouldSkipSendingEmail() || !cloudVersion || event.getUser().getCompany().isDemo()) {
            return;
        }
        try {
            String userEmail = event.getUser().getEmail();
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.PUT);
            request.setEndpoint("marketing/contacts");

            // Calculate trial end date (15 days from now)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 15);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
            String trialEndDate = dateFormat.format(calendar.getTime());

            // Prepare request body
            Map<String, Object> contact = new HashMap<>();
            contact.put("email", userEmail);
            contact.put("first_name", event.getUser().getFirstName());
            contact.put("last_name", event.getUser().getLastName());
            contact.put("trial_end_date", trialEndDate); // Add the calculated date

            Map<String, Object> body = new HashMap<>();
            body.put("contacts", Collections.singletonList(contact));
            body.put("list_ids", Collections.singletonList(contactListId));

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(body);

            request.setBody(jsonBody);
            Response response = sg.api(request);

            if (response.getStatusCode() >= 400) {
                log.error("SendGrid Marketing API error: Status={}, Body={}",
                        response.getStatusCode(), response.getBody());
                throw new CustomException("Failed to add user to SendGrid contacts",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.info("User added to SendGrid contact list successfully: {}", userEmail);

        } catch (IOException e) {
            log.error("Error adding user to SendGrid contacts", e);
            throw new CustomException("Failed to add user to SendGrid contacts",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Async
    public void removeUserFromContactList(String userEmail) {
        if (shouldSkipSendingEmail() || !cloudVersion) {
            return;
        }
        try {
            SendGrid sg = new SendGrid(sendGridApiKey);

            String safeEmail = userEmail.replace("'", "\\'");

            Request searchRequest = new Request();
            searchRequest.setMethod(Method.POST);
            searchRequest.setEndpoint("marketing/contacts/search");

            Map<String, Object> searchBody = new HashMap<>();
            searchBody.put("query", "email = '" + safeEmail + "'");

            searchRequest.setBody(objectMapper.writeValueAsString(searchBody));

            Response searchResponse = sg.api(searchRequest);

            if (searchResponse.getStatusCode() >= 400) {
                // Log error but DO NOT throw exception in void @Async
                log.error("SendGrid search failed for {}: Status={}, Body={}",
                        userEmail, searchResponse.getStatusCode(), searchResponse.getBody());
                return;
            }

            JsonNode responseNode = objectMapper.readTree(searchResponse.getBody());
            JsonNode resultNode = responseNode.get("result");

            if (resultNode == null || resultNode.isEmpty()) {
                log.warn("Contact email not found in SendGrid: {}", userEmail);
                return;
            }

            // Get the Contact ID
            String contactId = resultNode.get(0).get("id").asText();

            // 3. Remove contact from the specific list
            Request removeRequest = new Request();
            removeRequest.setMethod(Method.DELETE);
            removeRequest.setEndpoint("marketing/lists/" + contactListId + "/contacts");
            removeRequest.addQueryParam("contact_ids", contactId);

            Response removeResponse = sg.api(removeRequest);

            if (removeResponse.getStatusCode() >= 400) {
                log.error("SendGrid list removal failed for {}: Status={}, Body={}",
                        userEmail, removeResponse.getStatusCode(), removeResponse.getBody());
                return;
            }

            log.info("User {} removed from SendGrid list {} successfully.", userEmail, contactListId);

        } catch (Exception e) {
            log.error("Unexpected error removing user {} from SendGrid list", userEmail, e);
        }
    }

    public void sendSimpleMessage(String[] to, String subject, String text) {
        try {
            if (shouldSkipSendingEmail())
                return;
            Email from = new Email(fromEmail, fromName);
            Content content = new Content("text/plain", text);

            Mail mail = new Mail();
            mail.setFrom(from);
            mail.setSubject(subject);
            mail.addContent(content);

            // Add recipients
            Personalization personalization = new Personalization();
            for (String recipient : to) {
                personalization.addTo(new Email(recipient));
            }
            mail.addPersonalization(personalization);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 400) {
                log.error("SendGrid error: Status={}, Body={}",
                        response.getStatusCode(), response.getBody());
                throw new CustomException("Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.info("Email sent successfully. Status: {}", response.getStatusCode());

        } catch (IOException e) {
            log.error("Error sending email via SendGrid", e);
            throw new CustomException("Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean shouldSkipSendingEmail() {
        return Boolean.FALSE.equals(enableEmails) || MailService.skipMail.get();
    }

    /**
     * Send email with attachment
     */
    public void sendMessageWithAttachment(String to, String subject, String text,
                                          String attachmentName, byte[] attachmentData,
                                          String attachmentType) {
        try {
            if (shouldSkipSendingEmail())
                return;
            Email from = new Email(fromEmail, fromName);
            Email recipient = new Email(to);
            Content content = new Content("text/plain", text);
            Mail mail = new Mail(from, subject, recipient, content);

            // Add attachment
            Attachments attachments = new Attachments();
            String encodedAttachment = Base64.getEncoder().encodeToString(attachmentData);
            attachments.setContent(encodedAttachment);
            attachments.setType(attachmentType);
            attachments.setFilename(attachmentName);
            attachments.setDisposition("attachment");
            mail.addAttachments(attachments);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 400) {
                log.error("SendGrid error: Status={}, Body={}",
                        response.getStatusCode(), response.getBody());
                throw new CustomException("Failed to send email with attachment",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.info("Email with attachment sent successfully. Status: {}",
                    response.getStatusCode());

        } catch (IOException e) {
            log.error("Error sending email with attachment via SendGrid", e);
            throw new CustomException("Failed to send email with attachment",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send email using Thymeleaf template
     */
    @Async
    public void sendMessageUsingThymeleafTemplate(
            String[] to, String subject, Map<String, Object> templateModel,
            String template, Locale locale, List<EmailAttachmentDTO> attachmentDTOS) {
        if (to.length == 0) return;
        if (shouldSkipSendingEmail())
            return;
        Context thymeleafContext = new Context();
        thymeleafContext.setLocale(locale);
        thymeleafContext.setVariables(templateModel);
        thymeleafContext.setVariable("environment", environment);
        thymeleafContext.setVariable("brandConfig", brandingService.getBrandConfig());
        thymeleafContext.setVariable("backgroundColor", brandingService.getMailBackgroundColor());

        String htmlBody = thymeleafTemplateEngine.process(template, thymeleafContext);

        try {
            sendHtmlMessage(to, subject, htmlBody, attachmentDTOS, template);
        } catch (IOException e) {
            log.error("Error sending templated email", e);
            throw new CustomException("Can't send the mail", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send HTML email with optional attachments
     */
    private void sendHtmlMessage(String[] to, String subject, String htmlBody,
                                 List<EmailAttachmentDTO> attachmentDTOS, String template) throws IOException {
        if (shouldSkipSendingEmail())
            return;
        if (to != null && Arrays.stream(to).allMatch(recipient -> recipient != null && recipient.toLowerCase().endsWith("@demo.com")))
            return;
        Email from = new Email(fromEmail, fromName);
        Content content = new Content("text/html", htmlBody);

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(subject);
        mail.addContent(content);
        if (template != null) mail.addCategory(template);
        // Add recipients
        Personalization personalization = new Personalization();
        for (String recipient : to) {
            personalization.addTo(new Email(recipient));
        }
        mail.addPersonalization(personalization);

        // Add attachments if any
        if (attachmentDTOS != null && !attachmentDTOS.isEmpty()) {
            for (EmailAttachmentDTO attachmentDTO : attachmentDTOS) {
                Attachments attachment = new Attachments();
                String encodedAttachment = Base64.getEncoder()
                        .encodeToString(attachmentDTO.getAttachmentData());
                attachment.setContent(encodedAttachment);
                attachment.setType(attachmentDTO.getAttachmentType());
                attachment.setFilename(attachmentDTO.getAttachmentName());
                attachment.setDisposition("attachment");
                mail.addAttachments(attachment);
            }
        }

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);

        if (response.getStatusCode() >= 400) {
            log.error("SendGrid error: Status={}, Body={}",
                    response.getStatusCode(), response.getBody());
            throw new IOException("SendGrid API error: " + response.getStatusCode());
        }

        log.info("HTML email sent successfully. Status: {}", response.getStatusCode());
    }

    @Override
    public void sendHtmlMessage(String[] to, String subject, String htmlBody,
                                List<EmailAttachmentDTO> attachmentDTOS) throws IOException {
        sendHtmlMessage(to, subject, htmlBody, attachmentDTOS, null);
    }

    /**
     * Send email to super admins
     */
    public void sendMailToSuperAdmins(String subject, String text) {
        try {
            sendHtmlMessage(recipients, subject, text, null);
        } catch (IOException e) {
            log.error("Error sending email to super admins", e);
            throw new CustomException("Failed to send email to super admins",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}