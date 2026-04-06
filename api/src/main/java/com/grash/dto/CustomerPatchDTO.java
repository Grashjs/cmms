package com.grash.dto;

import com.grash.model.Currency;
import com.grash.model.abstracts.BasicInfos;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing customer")
public class CustomerPatchDTO extends BasicInfos {
    @Schema(description = "Type of vendor/customer")
    private String vendorType;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Hourly rate")
    private long rate;


    @Schema(description = "Billing name")
    private String billingName;

    @Schema(description = "Billing address line 1")
    private String billingAddress;

    @Schema(description = "Billing address line 2")
    private String billingAddress2;

    @Schema(description = "Currency for billing", implementation = IdDTO.class)
    private Currency billingCurrency;
}
