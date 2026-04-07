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
@Schema(description = "DTO for importing parts from external data sources")
public class PartImportDTO {

    @Schema(description = "Unique identifier")
    private Long id;
    
    @Schema(description = "Name")
    private String name;

    @Schema(description = "Cost")
    private double cost;

    @Schema(description = "Category name")
    private String category;

    @Schema(description = "Whether this is a non-stock part")
    private String nonStock;

    @Schema(description = "Barcode identifier")
    private String barcode;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Quantity in stock")
    private double quantity;

    @Schema(description = "Additional information")
    private String additionalInfos;

    @Schema(description = "Storage area")
    private String area;

    @Schema(description = "Minimum quantity threshold")
    private double minQuantity;

    @Schema(description = "Location name")
    private String locationName;
    
    @Schema(description = "List of assigned user emails")
    @Builder.Default
    private Collection<String> assignedToEmails = new ArrayList<>();
    
    @Schema(description = "List of team names")
    @Builder.Default
    private Collection<String> teamsNames = new ArrayList<>();
    
    @Schema(description = "List of customer names")
    @Builder.Default
    private Collection<String> customersNames = new ArrayList<>();
    
    @Schema(description = "List of vendor names")
    @Builder.Default
    private Collection<String> vendorsNames = new ArrayList<>();
}
