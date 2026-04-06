package com.grash.dto;

import com.grash.model.CompanySettings;
import com.grash.model.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO for displaying full company details in API responses")
public class CompanyShowDTO extends AuditShowDTO {
    @Schema(description = "Company name")
    private String name;
    
    @Schema(description = "Company address")
    private String address;
    
    @Schema(description = "Company phone number")
    private String phone;
    
    @Schema(description = "Company website")
    private String website;
    
    @Schema(description = "Company email")
    private String email;

    @Schema(description = "Number of employees")
    private int employeesCount;

    @Schema(description = "Company logo")
    private FileShowDTO logo;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State")
    private String state;

    @Schema(description = "ZIP code")
    private String zipCode;

    @Schema(description = "Company subscription information")
    private Subscription subscription;

    @Schema(description = "Company settings")
    private CompanySettings companySettings;

    @Schema(description = "Indicates whether this is a demo company")
    private boolean demo;
}
