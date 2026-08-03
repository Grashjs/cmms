package com.grash.model;

import com.grash.dto.IdDTO;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.ApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Purchase order entity for managing procurement")
public class PurchaseOrder extends CompanyAudit {
    @Schema(description = "Approval status of the purchase order")
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @NotNull
    @Schema(description = "Name of the purchase order", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private PurchaseOrderCategory category;

    @Schema(description = "Shipping due date")
    private Date shippingDueDate;

    @Schema(description = "Additional shipping details")
    private String shippingAdditionalDetail;

    @Schema(description = "Name of the ship-to recipient")
    private String shippingShipToName;

    @Schema(description = "Shipping company name")
    private String shippingCompanyName;

    @Schema(description = "Shipping address")
    private String shippingAddress;

    @Schema(description = "Shipping city")
    private String shippingCity;

    @Schema(description = "Shipping state")
    private String shippingState;

    @Schema(description = "Shipping zip code")
    private String shippingZipCode;

    @Schema(description = "Shipping phone number")
    private String shippingPhone;

    @Schema(description = "Shipping fax number")
    private String shippingFax;

    @Schema(description = "Additional info date")
    private Date additionalInfoDate;

    @Schema(description = "Name of the requisitioned person")
    private String additionalInfoRequisitionedName;

    @Schema(description = "Additional info shipping order category")
    private String additionalInfoShippingOrderCategory;

    @Schema(description = "Additional info terms")
    private String additionalInfoTerm;

    @Schema(description = "Additional notes")
    private String additionalInfoNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    private Vendor vendor;

    /**
     * The work order this purchase order was raised for, if any. One work order can lead to
     * several purchase orders — different vendors — so the key lives here rather than in a
     * join table. Null when the order was raised on its own, which is why existing orders
     * stay valid.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Schema(description = "The work order that triggered this purchase order",
            implementation = IdDTO.class)
    private WorkOrder workOrder;

    @Schema(description = "Indicates whether this is a demo purchase order")
    private boolean isDemo;

//    @ManyToOne
//    private Company requesterInformation;

}


