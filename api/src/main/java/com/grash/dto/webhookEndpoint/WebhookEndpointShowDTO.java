package com.grash.dto.webhookEndpoint;

import com.grash.dto.AuditShowDTO;
import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Status;
import com.grash.model.enums.webhook.PartField;
import com.grash.model.enums.webhook.WOField;
import com.grash.model.enums.webhook.WebhookEvent;
import lombok.Data;

import java.util.Collection;
import java.util.Date;

@Data
public class WebhookEndpointShowDTO extends AuditShowDTO {
    private String url;

    private String secret;

    private String code;


    private boolean enabled;

    private WebhookEvent event;

    private Date lastTriggeredAt;

    private Collection<AssetStatus> assetStatuses;

    private Collection<Status> workOrderStatuses;

    private Collection<WorkOrderCategory> workOrderCategories;

    private Collection<WOField> woFields;

    private Collection<PartField> partFields;

}