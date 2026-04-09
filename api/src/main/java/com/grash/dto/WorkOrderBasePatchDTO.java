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
@Schema(description = "Base DTO for patching work orders with common fields")
public class WorkOrderBasePatchDTO {

    @Schema(description = "Due date for the work order")
    private Date dueDate;
    @Schema(description = "Priority level of the work order")
    private Priority priority = Priority.NONE;
    @Schema(description = "Estimated duration to complete the work order (in hours)")
    private double estimatedDuration;
    @Schema(description = "Estimated start date for the work order")
    private Date estimatedStartDate;
    @Schema(description = "Detailed description of the work order", maxLength = 10000)
    private String description;
    @Schema(description = "Title of the work order", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    @Schema(description = "Whether a signature is required upon completion")
    private boolean requiredSignature;
    @Schema(description = "Image attached to the work order", implementation = IdDTO.class)
    private File image;
    @Schema(description = "Category/classification of the work order", implementation = IdDTO.class)
    private WorkOrderCategory category;
    @Schema(description = "Location where the work is to be performed", implementation = IdDTO.class)
    private Location location;

    @Schema(description = "Team assigned to the work order", implementation = IdDTO.class)
    private Team team;
    @Schema(description = "Primary user responsible for the work order", implementation = IdDTO.class)
    private User primaryUser;
    @ArraySchema(arraySchema = @Schema(description = "List of users assigned to the work order"), schema =
    @Schema(implementation = IdDTO.class))
    private List<User> assignedTo;
    @ArraySchema(arraySchema = @Schema(description = "List of customers associated with the work order"), schema =
    @Schema(implementation = IdDTO.class))
    private List<Customer> customers;
    @ArraySchema(arraySchema = @Schema(description = "List of files attached to the work order"), schema =
    @Schema(implementation = IdDTO.class))
    private List<File> files;
    @Schema(description = "Asset associated with the work order", implementation = IdDTO.class)
    private Asset asset;
}
