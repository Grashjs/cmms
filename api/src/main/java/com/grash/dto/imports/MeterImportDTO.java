package com.grash.dto.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO for importing meters from external data sources")
public class MeterImportDTO {

    @Schema(description = "Unique identifier")
    private Long id;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Unit of measurement")
    private String unit;

    @Schema(description = "Update frequency")
    private int updateFrequency;

    @Schema(description = "Meter category name")
    private String meterCategory;

    @Schema(description = "Location name")
    private String locationName;
    
    @Schema(description = "Asset name")
    private String assetName;
    
    @Schema(description = "List of user emails")
    @Builder.Default
    private Collection<String> usersEmails = new ArrayList<>();
}
