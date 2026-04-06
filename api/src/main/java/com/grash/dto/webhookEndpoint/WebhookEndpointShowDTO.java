package com.grash.dto.webhookEndpoint;

import com.grash.dto.AuditShowDTO;
import com.grash.model.enums.webhook.WebhookEvent;
import lombok.Data;

import java.util.Date;

@Data
public class WebhookEndpointShowDTO extends AuditShowDTO {
    private String url;

    private String secret;

    private String code;


    private boolean enabled;

    private WebhookEvent event;

    private Date lastTriggeredAt;

}