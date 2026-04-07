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
@Schema(description = "DTO for importing assets from external data sources")
public class AssetImportDTO {

    @Schema(description = "Unique identifier")
    private Long id;
    
    @Schema(description = "Whether the asset is archived")
    private String archived;
    
    @Schema(description = "Description")
    private String description;

    @Schema(description = "Location name")
    private String locationName;

    @Schema(description = "Parent asset name")
    private String parentAssetName;

    @Schema(description = "Area or zone")
    private String area;

    @Schema(description = "Barcode identifier")
    private String barCode;

    @Schema(description = "Category name")
    private String category;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Primary user email")
    private String primaryUserEmail;


    @Schema(description = "Warranty expiration date (timestamp)")
    private Double warrantyExpirationDate;

    @Schema(description = "Additional information")
    private String additionalInfos;

    @Schema(description = "Serial number")
    private String serialNumber;
    
    @Schema(description = "List of assigned user emails")
    @Builder.Default
    private Collection<String> assignedToEmails = new ArrayList<>();
    
    @Schema(description = "List of team names")
    @Builder.Default
    private Collection<String> teamsNames = new ArrayList<>();

    @Schema(description = "Asset status")
    private String status;

    @Schema(description = "Acquisition cost")
    private Double acquisitionCost;
    
    @Schema(description = "List of customer names")
    @Builder.Default
    private Collection<String> customersNames = new ArrayList<>();
    
    @Schema(description = "List of vendor names")
    @Builder.Default
    private Collection<String> vendorsNames = new ArrayList<>();
    
    @Schema(description = "List of part names")
    @Builder.Default
    private Collection<String> partsNames = new ArrayList<>();
    
    @Schema(description = "Model")
    private String model;
    
    @Schema(description = "Manufacturer")
    private String manufacturer;
    
    @Schema(description = "Power rating")
    private String power;
}
