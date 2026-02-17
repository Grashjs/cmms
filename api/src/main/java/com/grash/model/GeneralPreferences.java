package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.enums.BusinessType;
import com.grash.model.enums.DateFormat;
import com.grash.model.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "companySettings")
public class GeneralPreferences {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Language language = Language.EN;

    private DateFormat dateFormat = DateFormat.MMDDYY;

    @ManyToOne(fetch = FetchType.LAZY)
    private Currency currency;

    private BusinessType businessType = BusinessType.GENERAL_ASSET_MANAGEMENT;

    private String timeZone;

    private boolean autoAssignWorkOrders;

    private boolean autoAssignRequests;

    private boolean disableClosedWorkOrdersNotif;

    private boolean askFeedBackOnWOClosed = true;

    private boolean laborCostInTotalCost = true;

    private boolean woUpdateForRequesters = true;

    private boolean simplifiedWorkOrder;

    private int daysBeforePrevMaintNotification = 1;

    @NotBlank
    private String csvSeparator = ",";

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private CompanySettings companySettings;

    public GeneralPreferences(CompanySettings companySettings) {
        this.companySettings = companySettings;
    }


}

