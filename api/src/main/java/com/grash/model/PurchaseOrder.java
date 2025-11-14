package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.ApprovalStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
public class PurchaseOrder extends CompanyAudit {
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @NotNull
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
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

    @ManyToOne(fetch = FetchType.LAZY)
    private Vendor vendor;

//    @ManyToOne
//    private Company requesterInformation;

}
