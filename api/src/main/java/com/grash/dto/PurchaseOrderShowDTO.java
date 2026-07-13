package com.grash.dto;

import com.grash.model.PurchaseOrderCategory;
import com.grash.model.Vendor;
import com.grash.model.enums.ApprovalStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Date;

@Data
@NoArgsConstructor
public class PurchaseOrderShowDTO extends AuditShowDTO {

    private ApprovalStatus status = ApprovalStatus.PENDING;

    private String name;

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

    private Vendor vendor;

    private Collection<PartQuantityShowDTO> partQuantities;

}
