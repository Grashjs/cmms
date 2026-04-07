package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.enums.BusinessType;
import com.grash.model.enums.DateFormat;
import com.grash.model.enums.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.time.ZoneId;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "companySettings")
@Schema(description = "General preferences for company-wide settings and configurations")
public class GeneralPreferences {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Preferred language setting")
    private Language language = Language.EN;

    @Schema(description = "Date format preference")
    private DateFormat dateFormat = DateFormat.MMDDYY;

    @ManyToOne(fetch = FetchType.LAZY)
    private Currency currency;

    @Schema(description = "Type of business")
    private BusinessType businessType = BusinessType.GENERAL_ASSET_MANAGEMENT;

    @NotNull
    @Schema(description = "Time zone identifier", requiredMode = Schema.RequiredMode.REQUIRED)
    private String timeZone = ZoneId.systemDefault().getId();

    @Schema(description = "Automatically assign work orders")
    private boolean autoAssignWorkOrders;

    @Schema(description = "Automatically assign requests")
    private boolean autoAssignRequests;

    @Schema(description = "Disable notifications for closed work orders")
    private boolean disableClosedWorkOrdersNotif;

    @Schema(description = "Ask for feedback when work order is closed")
    private boolean askFeedBackOnWOClosed = true;

    @Schema(description = "Include labor cost in total cost calculation")
    private boolean laborCostInTotalCost = true;

    @Schema(description = "Allow work order updates for requesters")
    private boolean woUpdateForRequesters = true;

    @Schema(description = "Use simplified work order view")
    private boolean simplifiedWorkOrder;

    @Schema(description = "Days before preventive maintenance notification")
    private int daysBeforePrevMaintNotification = 1;

    @NotBlank
    @Schema(description = "CSV separator character", requiredMode = Schema.RequiredMode.REQUIRED)
    private String csvSeparator = ",";

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private CompanySettings companySettings;

    public GeneralPreferences(CompanySettings companySettings) {
        this.companySettings = companySettings;
    }


}

