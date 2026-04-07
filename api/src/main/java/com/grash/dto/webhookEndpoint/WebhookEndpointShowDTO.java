package com.grash.dto.webhookEndpoint;

import com.grash.dto.AuditShowDTO;
import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.ApprovalStatus;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Status;
import com.grash.model.enums.webhook.PartField;
import com.grash.model.enums.webhook.WOField;
import com.grash.model.enums.webhook.WebhookEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Collection;
import java.util.Date;

@Data
@Schema(description = "DTO for displaying webhook endpoint details in API responses")
public class WebhookEndpointShowDTO extends AuditShowDTO {
    @Schema(description = "Webhook URL")
    private String url;

    @Schema(description = "Secret key for webhook verification")
    private String secret;

    @Schema(description = "Webhook code")
    private String code;


    @Schema(description = "Whether the webhook is enabled")
    private boolean enabled;

    @Schema(description = "Webhook event type")
    private WebhookEvent event;

    @Schema(description = "Last triggered timestamp")
    private Date lastTriggeredAt;

    @Schema(description = "Filter by asset statuses")
    private Collection<AssetStatus> assetStatuses;

    @Schema(description = "Filter by work order statuses")
    private Collection<Status> workOrderStatuses;

    @Schema(description = "Filter by work order categories")
    private Collection<WorkOrderCategory> workOrderCategories;

    @Schema(description = "Work order fields to include")
    private Collection<WOField> woFields;

    @Schema(description = "Part fields to include")
    private Collection<PartField> partFields;

    @Schema(description = "Approval status")
    private Boolean approved;

    @Schema(description = "Whether the payload is serialized")
    private boolean serialize;

}