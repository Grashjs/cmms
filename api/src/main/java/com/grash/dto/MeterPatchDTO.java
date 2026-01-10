package com.grash.dto;

import com.grash.model.File;
import com.grash.model.Location;
import com.grash.model.MeterCategory;
import com.grash.model.OwnUser;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class MeterPatchDTO {
    private String name;

    private String unit;

    private int updateFrequency;

    @Schema(implementation = IdDTO.class)
    private MeterCategory meterCategory;

    @Schema(implementation = IdDTO.class)
    private File image;

    @Schema(implementation = IdDTO.class)
    private Location location;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<OwnUser> users;

}
