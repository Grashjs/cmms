package com.grash.dto;

import com.grash.model.*;
import com.grash.model.enums.AssetStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "DTO for partially updating an existing asset")
public class AssetPatchDTO {
    @Schema(description = "Indicates whether the asset is archived")
    private boolean archived;

    @Schema(description = "Image file associated with the asset", implementation = IdDTO.class)
    private File image;

    @Schema(description = "The location where the asset is situated", implementation = IdDTO.class)
    private Location location;

    @Schema(description = "The parent asset in a hierarchical structure", implementation = IdDTO.class)
    private Asset parentAsset;

    @Schema(description = "The area or zone where the asset is located")
    private String area;

    @Schema(description = "Barcode identifier for the asset")
    private String barCode;

    @Schema(description = "NFC (Near Field Communication) identifier for the asset")
    private String nfcId;

    @Schema(description = "The category of the asset", implementation = IdDTO.class)
    private AssetCategory category;

    @Schema(description = "The name of the asset")
    private String name;

    @Schema(description = "The primary user responsible for the asset", implementation = IdDTO.class)
    private User primaryUser;

    @Schema(description = "Depreciation configuration for the asset", implementation = IdDTO.class)
    private Deprecation deprecation;

    @Schema(description = "The warranty expiration date for the asset")
    private Date warrantyExpirationDate;

    @Schema(description = "Additional information about the asset")
    private String additionalInfos;

    @Schema(description = "The serial number of the asset")
    private String serialNumber;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of users assigned to the asset", writeOnly = true)
    )
    private Collection<User> assignedTo;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of customers associated with the asset", writeOnly = true)
    )
    private Collection<Customer> customers;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of vendors associated with the asset", writeOnly = true)
    )
    private Collection<Vendor> vendors;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of teams assigned to the asset", writeOnly = true)
    )
    private Collection<Team> teams;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of files attached to the asset", writeOnly = true)
    )
    private Collection<File> files;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of parts associated with the asset", writeOnly = true)
    )
    private Collection<Part> parts;

    @Schema(description = "The current status of the asset")
    private AssetStatus status;

    @Schema(description = "The acquisition cost of the asset")
    private Double acquisitionCost;

    @Schema(description = "The power rating or specification of the asset")
    private String power;

    @Schema(description = "The manufacturer of the asset")
    private String manufacturer;

    @Schema(description = "The model of the asset")
    private String model;

    @Schema(description = "Detailed description of the asset")
    private String description;

    @Schema(description = "The date when the asset was placed into service")
    private Date inServiceDate;
}
