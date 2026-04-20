package com.grash.dto;

import com.grash.model.File;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing company")
public class CompanyPatchDTO {

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
    
    @Schema(description = "Company logo", implementation = IdDTO.class)
    private File logo;
    
    @Schema(description = "City")
    private String city;
    
    @Schema(description = "State")
    private String state;
    
    @Schema(description = "ZIP code")
    private String zipCode;
}
