package com.grash.dto;

import com.grash.model.*;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@Schema(description = "DTO for partially updating an existing part")
public class PartPatchDTO {

    @Schema(description = "The name of the part")
    private String name;

    @Schema(description = "The cost of the part")
    private double cost;

    @Schema(description = "The category of the part", implementation = IdDTO.class)
    private PartCategory category;

    @Schema(description = "Indicates whether this is a non-stock part")
    private boolean nonStock;

    @Schema(description = "Barcode identifier for the part")
    private String barcode;

    @Schema(description = "Detailed description of the part")
    private String description;

    @Schema(description = "The current quantity of the part in stock")
    private double quantity;

    @Schema(description = "Additional information about the part")
    private String additionalInfos;

    @Schema(description = "The area or storage location where the part is kept")
    private String area;

    @Schema(description = "The minimum quantity threshold for the part")
    private double minQuantity;

    @Schema(description = "The location where the part is stored", implementation = IdDTO.class)
    private Location location;

    @Schema(description = "Image file associated with the part", implementation = IdDTO.class)
    private File image;

    @ArraySchema(
        schema = @Schema(implementation = IdDTO.class),
        arraySchema = @Schema(description = "List of users assigned to the part", writeOnly = true)
    )
    private Collection<OwnUser> assignedTo;

    @ArraySchema(
        schema = @Schema(implementation = IdDTO.class),
        arraySchema = @Schema(description = "List of files attached to the part", writeOnly = true)
    )
    private Collection<File> files;

    @ArraySchema(
        schema = @Schema(implementation = IdDTO.class),
        arraySchema = @Schema(description = "List of customers associated with the part", writeOnly = true)
    )
    private Collection<Customer> customers;

    @ArraySchema(
        schema = @Schema(implementation = IdDTO.class),
        arraySchema = @Schema(description = "List of vendors associated with the part", writeOnly = true)
    )
    private Collection<Vendor> vendors;

    @ArraySchema(
        schema = @Schema(implementation = IdDTO.class),
        arraySchema = @Schema(description = "List of teams assigned to the part", writeOnly = true)
    )
    private Collection<Team> teams;

    @Schema(description = "The unit of measurement for the part")
    private String unit;

}
