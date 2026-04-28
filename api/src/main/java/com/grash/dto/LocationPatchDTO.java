package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.model.*;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for partially updating an existing location")
public class LocationPatchDTO {
    @Schema(description = "The name of the location")
    private String name;

    @Schema(description = "The physical address of the location")
    private String address;

    @Schema(description = "The longitude coordinate of the location")
    private Double longitude;

    @Schema(description = "The latitude coordinate of the location")
    private Double latitude;

    @Schema(description = "The parent location in a hierarchical structure", implementation = IdDTO.class)
    private Location parentLocation;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of workers assigned to the location", writeOnly = true)
    )
    private Collection<User> workers;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of teams assigned to the location", writeOnly = true)
    )
    private Collection<Team> teams;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of vendors associated with the location", writeOnly = true)
    )
    private Collection<Vendor> vendors;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of customers associated with the location", writeOnly = true)
    )
    private Collection<Customer> customers;

    @Schema(description = "Image file associated with the location", implementation = IdDTO.class)
    private File image;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of files attached to the location", writeOnly = true)
    )
    private List<File> files = new ArrayList<>();

    @Schema(description = "Custom field values for the location")
    private List<CustomFieldValuePostDTO> customFields = new ArrayList<>();

}
