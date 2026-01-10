package com.grash.dto;

import com.grash.model.*;
import com.grash.model.enums.Priority;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class WorkOrderBasePatchDTO {

    private Date dueDate;
    private Priority priority = Priority.NONE;
    private double estimatedDuration;
    private Date estimatedStartDate;
    private String description;
    private String title;
    private boolean requiredSignature;
    @Schema(implementation = IdDTO.class)
    private File image;
    @Schema(implementation = IdDTO.class)
    private WorkOrderCategory category;
    @Schema(implementation = IdDTO.class)
    private Location location;

    @Schema(implementation = IdDTO.class)
    private Team team;
    @Schema(implementation = IdDTO.class)
    private OwnUser primaryUser;
    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private List<OwnUser> assignedTo;
    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private List<Customer> customers;
    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private List<File> files;
    @Schema(implementation = IdDTO.class)
    private Asset asset;
}
