package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Status;
import com.grash.model.enums.webhook.PartField;
import com.grash.model.enums.webhook.WOField;
import com.grash.model.enums.webhook.WebhookEvent;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Collection;
import java.util.Date;

@Entity
@Data
public class WebhookEndpoint extends CompanyAudit {
    @NotNull
    private String url;

    @NotNull
    private String secret;

    @NotNull
    private boolean enabled = true;

    private boolean serialize = false;

    @NotNull
    private WebhookEvent event;

    private Date lastTriggeredAt;

    private Collection<AssetStatus> assetStatuses;

    private Collection<Status> workOrderStatuses;

    private Collection<WorkOrderCategory> workOrderCategories;

    private Collection<WOField> woFields;

    private Collection<PartField> partFields;
}