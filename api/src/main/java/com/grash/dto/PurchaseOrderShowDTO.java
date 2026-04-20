package com.grash.dto;

import com.grash.model.PurchaseOrderCategory;
import com.grash.model.Vendor;
import com.grash.model.enums.ApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying purchase order details in API responses")
public class PurchaseOrderShowDTO extends AuditShowDTO {

    @Schema(description = "Approval status")
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Category")
    private CategoryMiniDTO category;

    @Schema(description = "Shipping due date")
    private Date shippingDueDate;

    @Schema(description = "Additional shipping details")
    private String shippingAdditionalDetail;

    @Schema(description = "Name of the recipient for shipping")
    private String shippingShipToName;

    @Schema(description = "Shipping company name")
    private String shippingCompanyName;

    @Schema(description = "Shipping address")
    private String shippingAddress;

    @Schema(description = "Shipping city")
    private String shippingCity;

    @Schema(description = "Shipping state")
    private String shippingState;

    @Schema(description = "Shipping ZIP code")
    private String shippingZipCode;

    @Schema(description = "Shipping phone number")
    private String shippingPhone;

    @Schema(description = "Shipping fax number")
    private String shippingFax;

    @Schema(description = "Additional info date")
    private Date additionalInfoDate;

    @Schema(description = "Name of the requisitioner")
    private String additionalInfoRequisitionedName;

    @Schema(description = "Additional info shipping order category")
    private String additionalInfoShippingOrderCategory;

    @Schema(description = "Additional info terms")
    private String additionalInfoTerm;

    @Schema(description = "Additional notes")
    private String additionalInfoNotes;

    @Schema(description = "Vendor information")
    private VendorMiniDTO vendor;

    @Schema(description = "Part quantities in the purchase order")
    private Collection<PartQuantityShowDTO> partQuantities;

}
