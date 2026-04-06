package com.grash.dto.webhookEndpoint;

import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Status;
import com.grash.model.enums.webhook.PartField;
import com.grash.model.enums.webhook.WOField;
import com.grash.model.enums.webhook.WebhookEvent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Collection;

@Data
public class WebhookEndpointPatchDTO {
    @NotNull
    private String url;

    private String code;

    private WebhookEvent event;

    private Collection<AssetStatus> assetStatuses;

    private Collection<Status> workOrderStatuses;

    private Collection<WorkOrderCategory> workOrderCategories;

    private Collection<WOField> woFields;

    private Collection<PartField> partFields;

}