package com.grash.dto;

import com.grash.exception.CustomException;
import com.grash.model.Currency;
import com.grash.model.enums.BusinessType;
import com.grash.model.enums.DateFormat;
import com.grash.model.enums.Language;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO for patching general preferences settings")
public class GeneralPreferencesPatchDTO {

    @Schema(description = "Language preference")
    private Language language;
    
    @Schema(description = "Currency", implementation = IdDTO.class)
    private Currency currency;
    
    @Schema(description = "Business type")
    private BusinessType businessType;
    
    @Schema(description = "Date format")
    private DateFormat dateFormat;
    
    @Schema(description = "Time zone")
    private String timeZone;
    
    @Schema(description = "Auto-assign work orders flag")
    private boolean autoAssignWorkOrders;
    
    @Schema(description = "Auto-assign requests flag")
    private boolean autoAssignRequests;
    
    @Schema(description = "Disable notifications for closed work orders")
    private boolean disableClosedWorkOrdersNotif;
    
    @Schema(description = "Ask for feedback on work order closed")
    private boolean askFeedBackOnWOClosed;
    
    @Schema(description = "Include labor cost in total cost")
    private boolean laborCostInTotalCost;
    
    @Schema(description = "Allow work order updates for requesters")
    private boolean woUpdateForRequesters;
    
    @Schema(description = "Simplified work order mode")
    private boolean simplifiedWorkOrder;
    
    @Schema(description = "Days before preventive maintenance notification")
    private int daysBeforePrevMaintNotification;
    
    @Schema(description = "CSV separator character")
    private String csvSeparator;

    public void setDaysBeforePrevMaintNotification(int daysBeforePrevMaintNotification) {
        if (daysBeforePrevMaintNotification < 0)
            throw new CustomException("Invalid daysBeforePrevMaintNotification", HttpStatus.BAD_REQUEST);
        this.daysBeforePrevMaintNotification = daysBeforePrevMaintNotification;
    }
}
