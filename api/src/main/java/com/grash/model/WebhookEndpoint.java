package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Status;
import com.grash.model.enums.webhook.PartField;
import com.grash.model.enums.webhook.WOField;
import com.grash.model.enums.webhook.WebhookEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Collection;
import java.util.Date;

@Entity
@Data
@Schema(description = "Webhook endpoint configuration for event notifications")
public class WebhookEndpoint extends CompanyAudit {
    @NotNull
    @Schema(description = "Webhook URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @NotNull
    @Schema(description = "Webhook secret for verification", requiredMode = Schema.RequiredMode.REQUIRED)
    private String secret;

    @NotNull
    @Schema(description = "Whether the webhook is enabled", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean enabled = true;

    @Schema(description = "Whether to serialize the webhook payload")
    private boolean serialize = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Schema(description = "Webhook event type", requiredMode = Schema.RequiredMode.REQUIRED)
    private WebhookEvent event;

    @Schema(description = "Last time the webhook was triggered", accessMode = Schema.AccessMode.READ_ONLY)
    private Date lastTriggeredAt;

    @ElementCollection(targetClass = AssetStatus.class)
    @Enumerated(EnumType.STRING)
    private Collection<AssetStatus> assetStatuses;

    @ElementCollection(targetClass = Status.class)
    @Enumerated(EnumType.STRING)
    private Collection<Status> workOrderStatuses;

    @ManyToMany
    private Collection<WorkOrderCategory> workOrderCategories;

    @ElementCollection(targetClass = WOField.class)
    @Enumerated(EnumType.STRING)
    private Collection<WOField> woFields;

    @ElementCollection(targetClass = PartField.class)
    @Enumerated(EnumType.STRING)
    private Collection<PartField> partFields;

    @Schema(description = "Whether the webhook is approved")
    private Boolean approved;
}