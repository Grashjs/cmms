package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Status;
import com.grash.model.enums.webhook.PartField;
import com.grash.model.enums.webhook.WOField;
import com.grash.model.enums.webhook.WebhookEvent;
import jakarta.persistence.*;
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
    @Enumerated(EnumType.STRING)
    private WebhookEvent event;

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

    private Boolean approved;
}