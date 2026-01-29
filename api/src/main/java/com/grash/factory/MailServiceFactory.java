package com.grash.factory;

import com.grash.model.enums.MailType;
import com.grash.service.EmailService2;
import com.grash.service.MailService;
import com.grash.service.SendgridService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MailServiceFactory {
    @Value("${mail.type:SMTP}")
    private MailType mailType;

    private final EmailService2 emailService2;
    private final SendgridService sendgridService;

    public MailService getMailService() {
        switch (mailType) {
            case SENDGRID:
                return sendgridService;
            default:
                return emailService2;
        }
    }
}
