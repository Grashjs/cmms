package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Minimal vendor information containing company name and ID")
public class VendorMiniDTO {
    @Schema(description = "Company name of the vendor")
    private String companyName;
    @Schema(description = "Unique identifier for the vendor")
    private Long id;
}
