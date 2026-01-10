package com.grash.dto;

import com.grash.model.*;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class PartPatchDTO {

    private String name;

    private double cost;

    @Schema(implementation = IdDTO.class)
    private PartCategory category;

    private boolean nonStock;

    private String barcode;

    private String description;

    private double quantity;

    private String additionalInfos;

    private String area;

    private double minQuantity;

    @Schema(implementation = IdDTO.class)
    private Location location;

    @Schema(implementation = IdDTO.class)
    private File image;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<OwnUser> assignedTo;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<File> files;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<Customer> customers;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<Vendor> vendors;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<Team> teams;

    private String unit;
    
}
