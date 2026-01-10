package com.grash.dto;

import com.grash.model.PurchaseOrderCategory;
import com.grash.model.Vendor;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class PurchaseOrderPatchDTO {
    private String name;

    @Schema(implementation = IdDTO.class)
    private PurchaseOrderCategory category;

    private Date shippingDueDate;

    private String shippingAdditionalDetail;

    private String shippingShipToName;

    private String shippingCompanyName;

    private String shippingAddress;

    private String shippingCity;

    private String shippingState;

    private String shippingZipCode;

    private String shippingPhone;

    private String shippingFax;

    private Date additionalInfoDate;

    private String additionalInfoRequisitionedName;

    private String additionalInfoShippingOrderCategory;

    private String additionalInfoTerm;

    private String additionalInfoNotes;

    @Schema(implementation = IdDTO.class)
    private Vendor vendor;

    // private Company requesterInformation;
}
