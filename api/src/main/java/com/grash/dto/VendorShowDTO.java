package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValueShowDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying full vendor details in API responses")
public class VendorShowDTO extends AuditShowDTO {

    @Schema(description = "Type of vendor")
    private String vendorType;

    @Schema(description = "Company name")
    private String companyName;

    @Schema(description = "Vendor description")
    private String description;

    @Schema(description = "Vendor rate")
    private long rate;

    @Schema(description = "Indicates whether this is a demo vendor")
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