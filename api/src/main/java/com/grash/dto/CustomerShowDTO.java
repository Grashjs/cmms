package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValueShowDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying full customer details in API responses")
public class CustomerShowDTO extends AuditShowDTO {

    @Schema(description = "Type of customer")
    private String customerType;

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

    @Schema(description = "Indicates whether this is a demo customer")
    private boolean isDemo;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Address")
    private String address;

    @Schema(description = "Phone")
    private String phone;

    @Schema(description = "Website")
    private String website;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Custom field values")
    private List<CustomFieldValueShowDTO> customFieldValues = new ArrayList<>();
}