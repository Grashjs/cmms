package com.grash.dto.webhookEndpoint;

import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Status;
import com.grash.model.enums.webhook.PartField;
import com.grash.model.enums.webhook.WOField;
import com.grash.model.enums.webhook.WebhookEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Collection;

@Data
@Schema(description = "DTO for creating a new webhook endpoint")
public class WebhookEndpointPostDTO {
    @Schema(description = "Webhook URL")
    @NotNull
    private String url;

    @Schema(description = "Secret code for webhook verification")
    private String code;

    @Schema(description = "Webhook event type")
    private WebhookEvent event;

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

    @Schema(description = "Approval status (for requests)")
    private Boolean approved;

    @Schema(description = "Whether to serialize the payload")
    private boolean serialize = false;


}