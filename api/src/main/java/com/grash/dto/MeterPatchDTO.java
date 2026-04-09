package com.grash.dto;

import com.grash.model.File;
import com.grash.model.Location;
import com.grash.model.MeterCategory;
import com.grash.model.User;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@Schema(description = "DTO for partially updating an existing meter")
public class MeterPatchDTO {
    @Schema(description = "The name of the meter")
    private String name;

    @Schema(description = "The unit of measurement for the meter")
    private String unit;

    @Schema(description = "The frequency at which the meter readings should be updated")
    private int updateFrequency;

    @Schema(description = "The category of the meter", implementation = IdDTO.class)
    private MeterCategory meterCategory;

    @Schema(description = "Image file associated with the meter", implementation = IdDTO.class)
    private File image;

    @Schema(description = "The location where the meter is installed", implementation = IdDTO.class)
    private Location location;

    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of users who have access to the meter", writeOnly = true)
    )
    private Collection<User> users;

}
