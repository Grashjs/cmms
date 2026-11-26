package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "webhookEndpoint")
public class WebhookDeliveryLog extends CompanyAudit {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private WebhookEndpoint webhookEndpoint;

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private Integer responseStatus;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    private Long durationMs;

    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
