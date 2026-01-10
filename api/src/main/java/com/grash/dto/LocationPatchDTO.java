package com.grash.dto;

import com.grash.model.*;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
public class LocationPatchDTO {
    private String name;

    private String address;

    private Double longitude;

    private Double latitude;

    @Schema(implementation = IdDTO.class)
    private Location parentLocation;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<OwnUser> workers;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<Team> teams;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<Vendor> vendors;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<Customer> customers;

    @Schema(implementation = IdDTO.class)
    private File image;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private List<File> files = new ArrayList<>();

}
