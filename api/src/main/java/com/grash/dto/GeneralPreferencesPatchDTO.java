package com.grash.dto;

import com.grash.exception.CustomException;
import com.grash.model.Currency;
import com.grash.model.enums.BusinessType;
import com.grash.model.enums.DateFormat;
import com.grash.model.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralPreferencesPatchDTO {

    private Language language;
    private Currency currency;
    private BusinessType businessType;
    private DateFormat dateFormat;
    private String timeZone;
    private boolean autoAssignWorkOrders;
    private boolean autoAssignRequests;
    private boolean disableClosedWorkOrdersNotif;
    private boolean askFeedBackOnWOClosed;
    private boolean laborCostInTotalCost;
    private boolean woUpdateForRequesters;
    private boolean simplifiedWorkOrder;
    private int daysBeforePrevMaintNotification;

    public void setDaysBeforePrevMaintNotification(int daysBeforePrevMaintNotification) {
        if (daysBeforePrevMaintNotification < 0)
            throw new CustomException("Invalid daysBeforePrevMaintNotification", HttpStatus.BAD_REQUEST);
        this.daysBeforePrevMaintNotification = daysBeforePrevMaintNotification;
    }
}
