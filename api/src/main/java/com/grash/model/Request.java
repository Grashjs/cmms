package com.grash.model;

import com.grash.model.abstracts.WorkOrderBase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Maintenance request entity representing a submitted request for maintenance work")
public class Request extends WorkOrderBase {
    @Schema(description = "Unique identifier of the request", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Schema(description = "Custom identifier for the request")
    private String customId;

    @Schema(description = "Whether the request has been cancelled")
    private boolean cancelled;

    @Schema(description = "Reason provided for cancelling the request")
    private String cancellationReason;

    @Schema(description = "Indicates whether this is a demo request")
    private boolean isDemo;

    @Schema(description = "Audio description file attached to the request")
    @OneToOne(fetch = FetchType.LAZY)
    private File audioDescription;

    @Schema(description = "Work order created from this request (if approved)")
    @OneToOne(fetch = FetchType.LAZY)
    private WorkOrder workOrder;

    @Schema(description = "Portal through which this request was submitted")
    @ManyToOne(fetch = FetchType.LAZY)
    private RequestPortal requestPortal;

    @Schema(description = "Contact information of the person who submitted the request")
    private String contact;

    @PreRemove
    private void preRemove() {
        if (workOrder != null)
            workOrder.setParentRequest(null);
    }

}

